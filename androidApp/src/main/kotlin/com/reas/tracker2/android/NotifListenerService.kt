package com.reas.tracker2.android

import android.app.PendingIntent
import android.app.TaskStackBuilder
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
import com.reas.tracker2.MainActivity
import com.reas.tracker2.R
import com.reas.tracker2.database.Repository
import com.reas.tracker2.network.TrackerInstanceClient
import com.reas.tracker2.settings.Settings
import com.reas.tracker2.settings.isScrobblingEnabled
import com.reas.tracker2.shared.Event
import com.reas.tracker2.shared.EventProcessor
import com.reas.tracker2.shared.EventState
import com.reas.tracker2.shared.Source
import com.reas.tracker2.util.InMemoryLog
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.milliseconds

private val MediaMetadata.title
    get() = this.getString(MediaMetadata.METADATA_KEY_TITLE)
private val MediaMetadata.artist
    get() = this.getString(MediaMetadata.METADATA_KEY_ARTIST)
private val MediaMetadata.album
    get() = this.getString(MediaMetadata.METADATA_KEY_ALBUM)
private val MediaMetadata.albumArtist
    get() = this.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
private val MediaMetadata.duration
    get() = this.getLong(MediaMetadata.METADATA_KEY_DURATION)

private object NotificationListenerService {
    val logger = KotlinLogging.logger {}
}

private class MediaCallback(private val appId: String): MediaController.Callback(), KoinComponent {
    private val logger = com.reas.tracker2.android.NotificationListenerService.logger
    private val inMemoryLogger: InMemoryLog by inject()
    private val repository: Repository by inject()
    private val notificationManager: NotificationWrapper by inject()
    private val syncManager: TrackerInstanceClient by inject()
    private val eventProcessor: EventProcessor by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentMetadata: MediaMetadata? = null
    private var currentState: PlaybackState? = null
    private var lastEvent: Event? = null
    private var lastPlaybackRate = 0f

    // TODO: this doesn't work, rethink
    private var sentEvent: Boolean = false

    private fun updateNotification(event: Event) {
        val notificationBuilder: NotificationBuilder = if (event.isPlaying) {
            { context ->
                setContentTitle(event.track)
                setContentText(event.artistsAsString)
                setSmallIcon(R.drawable.ic_stat_name)
                setShowWhen(false)

                val resultIntent = Intent(context, MainActivity::class.java)
                val resultPendingIntent =
                    TaskStackBuilder.create(context).run {
                        addNextIntentWithParentStack(resultIntent)
                        getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    }
                setContentIntent(resultPendingIntent)

//                val deleteIntent = Intent(context, NotifListenerService::class.java)
//                deleteIntent.putExtra("org.reas.tracker2.appId", appId)
//                val deletePendingIntent = PendingIntent.getService(context, 42, deleteIntent,
//                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
//                setDeleteIntent(deletePendingIntent)
            }
        } else {
            {
                setContentTitle("Nothing is playing")
                setSmallIcon(R.drawable.ic_stat_name)
                setShowWhen(false)
            }
        }
        notificationManager.show(
            "Now Playing",
            NotificationWrapper.PLAYING_ID,
            notificationBuilder
        )
    }

    private fun addEvent() {
        if (currentMetadata == null || currentState == null)
            return
        val metadata = currentMetadata!!
        val state = currentState!!
        if (metadata.artist.isNullOrBlank() || metadata.title.isNullOrBlank() || state.state == PlaybackState.STATE_NONE)
            return

        if (sentEvent) return
        sentEvent = true

        val isPlaying = state.state == PlaybackState.STATE_PLAYING

        val event = Event.create(
            track = metadata.title!!,
            artists = metadata.artist!!,
            album = metadata.album,
            albumArtists = metadata.albumArtist ?: metadata.artist,
            duration = metadata.duration,
            timestamp = state.lastPositionUpdateTime - SystemClock.elapsedRealtime() + System.currentTimeMillis(),
            position = if (isPlaying && state.position < EventProcessor.SKIP_MIN_DURATION.inWholeMilliseconds) 0L else state.position,
            state = if (isPlaying) EventState.PLAYING else EventState.STOPPED,
            source = Source.local(appId)
        )

        // optimization to store less events
        if (lastEvent == null && !event.isPlaying)
            return
        if (lastEvent != null) {
            val l = lastEvent!!
            if (!l.isPlaying && !event.isPlaying)
                return
            if (l.isPlaying && event.isPlaying
                && l.metadata == event.metadata
                && state.playbackSpeed == lastPlaybackRate
                && ((event.timestamp - l.timestamp) * lastPlaybackRate.toDouble() - (event.position - l.position)).absoluteValue < 50.milliseconds) {
                return
            }
        }
        lastEvent = event
        lastPlaybackRate = state.playbackSpeed

        scope.launch {
            val plays = eventProcessor.process(listOf(event))
            plays.lastOrNull()?.let { lastPlay ->
                val processedEvent = event.copy(metadata = lastPlay.metadata)
                repository.insertEvent(processedEvent)
                syncManager.submitEvent(processedEvent)
                repository.insertPlays(plays)
                updateNotification(processedEvent)
            }
        }
    }

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
        log { """
            onMetadataChanged      ($appId)
                    track=${metadata?.title}
                    artist=${metadata?.artist}
                    album=${metadata?.album}
                    albumArtist=${metadata?.albumArtist}
                    duration=${metadata?.duration}
        """.trimIndent() }

        if (metadata == null) return
        currentMetadata = metadata
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
        log { """
            onPlaybackStateChanged ($appId)
                    lastPositionUpdateTime=${state?.lastPositionUpdateTime}
                    position=${state?.position}
                    state=${getStringForStateInt(state?.state)}
                    playbackSpeed=${state?.playbackSpeed}
        """.trimIndent() }

        if (state == null) return
        currentState = state
        sentEvent = false
        addEvent()
    }

    fun onDisconnect() {
        log { "onDisconnect           ($appId)" }
        currentState = currentState?.let {
            PlaybackState.Builder(it).setState(
                PlaybackState.STATE_STOPPED,
                it.position + SystemClock.elapsedRealtime() - it.lastPositionUpdateTime,
                1.0f
            ).build()
        }
        sentEvent = false
        addEvent()
    }

//    fun onNotificationDismissed() {
//        notificationId = notificationManager.reserveId()
//        showNotification()
//    }
}

private class SessionListener: MediaSessionManager.OnActiveSessionsChangedListener, KoinComponent {
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

//    fun onNotificationDismissed(appId: String) {
//        Logger.d(TAG) { "onNotificationDismissed($appId)" }
//        callbacks[appId]?.onNotificationDismissed()
//    }
}

class NotifListenerService: NotificationListenerService(), KoinComponent {
    private val logger = com.reas.tracker2.android.NotificationListenerService.logger
    private var initialized = false
    private var listener: SessionListener? = null
    private val inMemoryLogger: InMemoryLog by inject()
    private val notificationWrapper: NotificationWrapper by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        if (startId == 42)
//            listener!!.onNotificationDismissed(intent!!.getStringExtra("org.reas.tracker2.appId")!!)
        ServiceCompat.startForeground(
            this,
            NotificationWrapper.PLAYING_ID,
            notificationWrapper.notification("Now Playing") {
                setContentTitle("Nothing is playing")
                setSmallIcon(R.drawable.ic_stat_name)
                setShowWhen(false)
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            else
                0
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