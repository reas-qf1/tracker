package com.reas.tracker2.android

import android.media.MediaMetadata
import android.media.session.PlaybackState
import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.Event
import com.reas.tracker2.shared.EventProcessor
import com.reas.tracker2.shared.EventState
import com.reas.tracker2.shared.Source
import com.reas.tracker2.util.NowPlayingNotificationManager
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class MediaEventRelay(
    private val repository: Repository,
    private val eventProcessor: EventProcessor,
    private val notificationManager: NowPlayingNotificationManager,
) {
    private val GRACE_PERIOD = 50.milliseconds

    private val logger = KotlinLogging.logger { }
    private var lastEvent: Event? = null
    private val lock = Mutex()

    fun filter(
        event: Event,
        lastEvent: Event?,
    ): Boolean {
        if (lastEvent == null && !event.isPlaying) {
            return false
        }
        if (lastEvent != null) {
            if (!lastEvent.isPlaying && !event.isPlaying) {
                return false
            }
            if (lastEvent.isPlaying && event.isPlaying &&
                lastEvent.metadata == event.metadata &&
                event.speed == lastEvent.speed &&
                ((event.timestamp - lastEvent.timestamp) * event.speed - (event.position - lastEvent.position)).absoluteValue <
                GRACE_PERIOD
            ) {
                return false
            }
        }
        return true
    }

    fun map(
        event: Event,
        lastEvent: Event?,
    ): Event {
        var newEvent = event
        if (event.isPlaying && event.position < EventProcessor.SKIP_MIN_DURATION) {
            newEvent = newEvent.copy(info = newEvent.info.copy(position = Duration.ZERO))
        }
        return newEvent
    }

    suspend fun process(
        appId: String,
        timestamp: Long,
        metadata: MediaMetadata?,
        state: PlaybackState?,
    ) {
        if (metadata == null || state == null) {
            return
        }
        if (metadata.artist.isNullOrBlank() || metadata.title.isNullOrBlank() || state.state == PlaybackState.STATE_NONE) {
            return
        }

        val isPlaying = state.state == PlaybackState.STATE_PLAYING

        val event =
            Event.create(
                track = metadata.title!!,
                artists = metadata.artist!!,
                album = metadata.album,
                albumArtists = metadata.albumArtist ?: metadata.artist,
                duration = metadata.duration,
                timestamp = timestamp,
                position = state.position,
                state = if (isPlaying) EventState.PLAYING else EventState.STOPPED,
                speed = state.playbackSpeed.toDouble(),
                source = Source.local(appId),
            )

        lock.withLock {
            if (!filter(event, lastEvent)) {
                return
            }
            lastEvent = event

            val savedEvent = map(event, lastEvent)
            val plays = eventProcessor.process(listOf(savedEvent))
            plays.lastOrNull()?.let { lastPlay ->
                val processedEvent = savedEvent.copy(metadata = lastPlay.metadata)
                repository.insertPlays(plays)
                notificationManager.show(processedEvent)
            }
        }
    }
}
