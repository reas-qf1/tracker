package com.reas.tracker2.android

import android.media.MediaMetadata
import android.media.session.PlaybackState
import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.*
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
    private val GRACE_PERIOD = 500.milliseconds

    private val logger = KotlinLogging.logger { }
    private var lastEvent: Event? = null
    private val lock = Mutex()

    fun filter(event: Event, lastEvent: Event?): Boolean {
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

    fun map(event: Event, lastEvent: Event?): Event {
        var newEvent = event
        if (event.isPlaying && event.position < EventProcessor.SKIP_MIN_DURATION) {
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

        val isPlaying = state.state == PlaybackState.STATE_PLAYING
        val trackObject = Track(metadata.title!!, metadata.artist!!, metadata.album, metadata.albumArtist)
        // i don't like this, we do 2x more db calls than necessary
        val trackId = repository.getOrInsertTrack(trackObject)
        val metadataWithIds = repository.getTrack(trackId)

        val event = Event.create(
            track = metadataWithIds,
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
            val savedEvent = map(event, lastEvent)
            lastEvent = savedEvent

            val (newPlays, updatedPlays) = eventProcessor.process(listOf(savedEvent))
            logger.debug { "${newPlays.size} new plays, ${updatedPlays.size} updated plays" }

            repository.insertScrobbles(newPlays)
            repository.updateScrobbleEvents(updatedPlays)
            newPlays.forEach { play ->
                repository.insertEdit(ScrobbleEdit.new(play))
            }

            val notifPlay = newPlays.lastOrNull { !it.isDeleted } ?: updatedPlays.lastOrNull { !it.isDeleted }
            notifPlay?.let { play ->
                notificationManager.show(
                    repository.getScrobble(play.id)
                        ?: throw IllegalStateException("can't find play by id ${play.id}"),
                )
            }
        }
    }
}
