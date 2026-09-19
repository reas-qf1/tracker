package com.reas.tracker2.android

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import com.reas.tracker2.R
import com.reas.tracker2.settings.Settings
import com.reas.tracker2.settings.isScrobblingEnabled
import com.reas.tracker2.util.InMemoryLog
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

private object NotificationListenerService {
    val logger = KotlinLogging.logger {}
}

private class MediaCallback(
    private val appId: String,
) : MediaController.Callback(),
    KoinComponent {
    private val logger = com.reas.tracker2.android.NotificationListenerService.logger
    private val inMemoryLogger: InMemoryLog by inject()
    private val mediaEventProcessor: MediaEventRelay by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentMetadata: MediaMetadata? = null
    private var currentState: PlaybackState? = null
    private var lastUpdateTime = 0L
    private val isSending = AtomicBoolean(false)

    private fun log(message: () -> String) {
        logger.debug(message)
        scope.launch {
            inMemoryLogger.log("MediaEvents", message)
        }
    }

    fun onConnect(controller: MediaController) {
        log { "onConnect              ($appId)" }
        controller.metadata?.let { onMetadataChanged(it) }
        controller.playbackState?.let { onPlaybackStateChanged(it) }
    }

    override fun onMetadataChanged(metadata: MediaMetadata?) {
        log {
            """
            onMetadataChanged      ($appId)
                    track=${metadata?.title}
                    artist=${metadata?.artist}
                    album=${metadata?.album}
                    albumArtist=${metadata?.albumArtist}
                    duration=${metadata?.duration}
            """.trimIndent()
        }

        if (metadata == null) return
        currentMetadata = metadata
        lastUpdateTime = System.currentTimeMillis()
        addEvent()
    }

    private fun getStringForStateInt(state: Int?): String {
        when (state) {
            null -> return "null"
            PlaybackState.STATE_NONE -> return "NONE"
            PlaybackState.STATE_STOPPED -> return "STOPPED"
            PlaybackState.STATE_PAUSED -> return "PAUSED"
            PlaybackState.STATE_PLAYING -> return "PLAYING"
            PlaybackState.STATE_FAST_FORWARDING -> return "FAST_FORWARDING"
            PlaybackState.STATE_REWINDING -> return "REWINDING"
            PlaybackState.STATE_BUFFERING -> return "BUFFERING"
            PlaybackState.STATE_ERROR -> return "ERROR"
            PlaybackState.STATE_CONNECTING -> return "CONNECTING"
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS -> return "SKIPPING_TO_PREVIOUS"
            PlaybackState.STATE_SKIPPING_TO_NEXT -> return "SKIPPING_TO_NEXT"
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> return "SKIPPING_TO_QUEUE_ITEM"
            else -> return "UNKNOWN"
        }
    }

    override fun onPlaybackStateChanged(state: PlaybackState?) {
        log {
            """
            onPlaybackStateChanged ($appId)
                    lastPositionUpdateTime=${state?.lastPositionUpdateTime}
                    position=${state?.position}
                    state=${getStringForStateInt(state?.state)}
                    playbackSpeed=${state?.playbackSpeed}
            """.trimIndent()
        }

        if (state == null) return
        currentState = state
        lastUpdateTime = state.lastPositionUpdateTime - SystemClock.elapsedRealtime() + System.currentTimeMillis()
        addEvent()
    }

    fun onDisconnect() {
        log { "onDisconnect           ($appId)" }
        currentState =
            currentState?.let {
                PlaybackState
                    .Builder(it)
                    .setState(
                        PlaybackState.STATE_STOPPED,
                        it.position + SystemClock.elapsedRealtime() - it.lastPositionUpdateTime,
                        1.0f,
                    ).build()
            }
        lastUpdateTime = System.currentTimeMillis()
        addEvent()
    }

    private fun addEvent() {
        scope.launch {
            // this here is an insane hack to deal with onMetadataChanged and onPlaybackStateChanged
            // coming in at an arbitrary order when both were changed at the same time

            // here isSending is set to true, and the first function to be called waits for a while
            // so that the second function can get to this point too
            if (isSending.compareAndSet(false, true)) {
                delay(150.milliseconds) // delay time subject to change
            }
            // here one of the functions sets isSending to false, and the other returns prematurely
            if (!isSending.compareAndSet(true, false)) {
                return@launch
            }
            // by this point all current fields have been updated, and only one of the two methods
            // reached here, so we can safely save the event
            mediaEventProcessor.process(appId, lastUpdateTime, currentMetadata, currentState)
        }
    }
}

private class SessionListener :
    MediaSessionManager.OnActiveSessionsChangedListener,
    KoinComponent {
    private val logger = com.reas.tracker2.android.NotificationListenerService.logger
    private val inMemoryLogger: InMemoryLog by inject()
    private val settings: Settings by inject()
    private val controllers = mutableMapOf<String, MediaController>()
    private var callbacks = mutableMapOf<String, MediaCallback>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            settings.collect(isScrobblingEnabled) { value ->
                if (value) {
                    logger.debug { "scrobbling enabled via settings" }
                    callbacks.forEach { (appId, callback) ->
                        val controller = controllers[appId]!!
                        withContext(Dispatchers.Main) {
                            controller.registerCallback(callback)
                            callback.onConnect(controller)
                        }
                    }
                } else {
                    logger.debug { "scrobbling disabled via settings" }
                    callbacks.forEach { (appId, callback) ->
                        val controller = controllers[appId]!!
                        withContext(Dispatchers.Main) {
                            callback.onDisconnect()
                            controller.unregisterCallback(callback)
                        }
                    }
                }
            }
        }
    }

    private fun log(message: () -> String) {
        logger.debug(message)
        scope.launch {
            inMemoryLogger.log("MediaEvents", message)
        }
    }

    override fun onActiveSessionsChanged(ctrl: List<MediaController>?) {
        log { "onActiveSessionsChanged ${ctrl?.map { it.packageName }}" }
        if (ctrl == null) return

        val oldControllers = controllers.keys
        val newControllerMap = ctrl.associateBy { it.packageName }
        val newControllers = newControllerMap.keys

        val isScrobbling = settings.getBlocking(isScrobblingEnabled)

        // rewire callbacks for any refreshed controllers with the same package name
        // (happens e.g. if the service gets restarted)
        oldControllers.intersect(newControllers).forEach { appId ->
            if (controllers[appId] != newControllerMap[appId]) {
                val callback = callbacks[appId]!!
                if (isScrobbling) {
                    controllers[appId]!!.unregisterCallback(callback)
                    newControllerMap[appId]!!.registerCallback(callback)
                }
                controllers[appId] = newControllerMap[appId]!!
            }
        }

        // remove callbacks for disconnected session controllers
        oldControllers.minus(newControllers).forEach { appId ->
            val callback = callbacks[appId]!!
            if (isScrobbling) {
                callback.onDisconnect()
                controllers[appId]!!.unregisterCallback(callback)
            }
            callbacks.remove(appId)
            controllers.remove(appId)
        }

        // add callbacks for new session controllers
        newControllers.minus(oldControllers).forEach { appId ->
            val callback = MediaCallback(appId)
            val controller = newControllerMap[appId]!!
            if (isScrobbling) {
                controller.registerCallback(callback)
                callback.onConnect(controller)
            }
            callbacks[appId] = callback
            controllers[appId] = controller
        }
    }
}

class NotifListenerService :
    NotificationListenerService(),
    KoinComponent {
    private val logger = com.reas.tracker2.android.NotificationListenerService.logger
    private var initialized = false
    private var listener: SessionListener? = null
    private val inMemoryLogger: InMemoryLog by inject()
    private val notificationWrapper: NotificationWrapper by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        ServiceCompat.startForeground(
            this,
            NotificationWrapper.PLAYING_ID,
            notificationWrapper.notification("Now Playing") {
                setContentTitle("Nothing is playing")
                setSmallIcon(R.drawable.ic_stat_name)
                setShowWhen(false)
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        return START_STICKY
    }

    private fun log(message: () -> String) {
        logger.debug(message)
        scope.launch {
            inMemoryLogger.log("MediaEvents", message)
        }
    }

    private fun init() {
        if (listener != null) return
        val sessManager = getSystemService<MediaSessionManager>()!!
        val component = ComponentName(this, this::class.java)
        listener = SessionListener()

        sessManager.addOnActiveSessionsChangedListener(listener!!, component)
        listener!!.onActiveSessionsChanged(sessManager.getActiveSessions(component))
    }

    private fun destroy() {
        val sessManager = getSystemService<MediaSessionManager>()!!
        sessManager.removeOnActiveSessionsChangedListener(listener!!)
        listener = null
        initialized = false
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        log { "onListenerConnected" }
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    initialized = true
                    init()
                }
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        log { "onListenerDisconnected" }
        destroy()
    }
}
