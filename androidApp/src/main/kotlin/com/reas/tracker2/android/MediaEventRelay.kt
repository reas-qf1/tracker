package com.reas.tracker2.android

import android.media.MediaMetadata
import android.media.session.PlaybackState
import com.reas.tracker2.internalsServer.EventProcessor
import com.reas.tracker2.internalsServer.LocalServer
import com.reas.tracker2.shared.*
import com.reas.tracker2.util.NowPlayingNotificationManager
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class MediaEventRelay(
    private val server: LocalServer,
    private val notificationManager: NowPlayingNotificationManager,
) {
    private val GRACE_PERIOD = 500.milliseconds

    private val logger = KotlinLogging.logger {}
    private var lastEvent: RawEvent? = null
    private val lock = Mutex()

    fun filter(event: RawEvent, lastEvent: RawEvent?): Boolean {
        if (lastEvent == null && !event.isPlaying) {
            return false
        }
        if (lastEvent != null) {
            if (!lastEvent.isPlaying && !event.isPlaying) {
                return false
            }
            if (lastEvent.isPlaying && event.isPlaying && lastEvent.metadata == event.metadata) {
                val expectedDiff = event.timestamp - lastEvent.timestamp
                val actualDiff = (event.position - lastEvent.position) / event.speed
                if ((expectedDiff - actualDiff).absoluteValue < GRACE_PERIOD) {
                    return false
                }
            }
        }
        return true
    }

    fun map(event: RawEvent, lastEvent: RawEvent?): RawEvent {
        var newEvent = event
        if (event.isPlaying && event.position < EventProcessor.START_GRACE_PERIOD) {
            newEvent = newEvent.copy(info = newEvent.info.copy(position = Duration.ZERO))
        }
        if (lastEvent != null && lastEvent.metadata == event.metadata) {
            if (lastEvent.isPlaying) {
                val expectedDiff = event.timestamp - lastEvent.timestamp
                val actualDiff = (event.position - lastEvent.position) / event.speed
                if ((expectedDiff - actualDiff).absoluteValue < GRACE_PERIOD) {
                    val actualPos = lastEvent.position + expectedDiff * event.speed
                    newEvent = newEvent.copy(info = newEvent.info.copy(position = actualPos))
                }
            } else {
                if ((lastEvent.position - event.position).absoluteValue < GRACE_PERIOD) {
                    newEvent = newEvent.copy(info = newEvent.info.copy(position = lastEvent.position))
                }
            }
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

        val event = RawEvent(
            metadata = RawMetadata.create(
                track = metadata.title!!,
                artists = RawArtistList.create(metadata.artist!!),
                album = metadata.album,
                albumArtists = metadata.albumArtist?.let { RawArtistList.create(it) },
            ),
            duration = metadata.duration.milliseconds,
            info = EventInfo(
                timestamp = Instant.fromEpochMilliseconds(timestamp),
                position = state.position.milliseconds,
                state = if (state.state == PlaybackState.STATE_PLAYING) EventState.PLAYING else EventState.STOPPED,
                speed = state.playbackSpeed.toDouble(),
            ),
            source = Source.local(appId),
        )

        lock.withLock {
            if (!filter(event, lastEvent)) {
                return
            }
            val savedEvent = map(event, lastEvent)
            lastEvent = event

            val notifScrobble = server.submit(listOf(savedEvent)).singleOrNull()
            if (notifScrobble != null) {
                notificationManager.show(notifScrobble)
            } else {
                logger.debug { "can't update notif: submit returned 0 or >1 scrobbles" }
            }
        }
    }
}
