package com.reas.tracker2.shared

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration.Companion.seconds

class EventProcessor(
    private val adapter: EventProcessorAdapter
) {
    companion object {
        val SKIP_MIN_DURATION = 2.seconds
        private val logger = KotlinLogging.logger {}
    }

    // TODO: these should probably be a preference in order to persist between restarts
    private val temporaryEdits = hashMapOf<Source, Pair<Play, TrackWithAlbum?>>()

    suspend fun getLastPlay(source: Source): Play? {
        val dbPlay = revertTemporaryEdit(adapter.getLastPlayFromSource(source))
        val deletedPlay = temporaryEdits[source]?.first

        return if (dbPlay != null && deletedPlay != null) {
            if (dbPlay.timestamp > deletedPlay.timestamp) dbPlay else deletedPlay
        } else {
            dbPlay ?: deletedPlay
        }
    }

    suspend fun addTemporaryEdit(play: Play, newMetadata: TrackWithAlbum?): Boolean {
        val source = play.source
        val lastPlay = getLastPlay(source)
        val isLastPlay = lastPlay != null && lastPlay.timestamp == play.timestamp
        if (isLastPlay) {
            logger.debug { "addTemporaryEdit $play $newMetadata" }
            val originalPlay = temporaryEdits[source]?.first ?: play
            temporaryEdits[source] = originalPlay to newMetadata
        }
        return isLastPlay
    }

    private fun revertTemporaryEdit(play: Play?): Play? {
        if (play == null) return null
        var newPlay = play
        temporaryEdits[play.source]?.let {
            newPlay = play.copy(metadata = it.first.metadata)
        }
        return newPlay
    }

    private fun MutableList<Play>.applyTemporaryEditAndAdd(play: Play) {
        val temporaryEdit = temporaryEdits[play.source]
        if (temporaryEdit != null) {
            if (temporaryEdit.second != null)
                this.add(play.copy(metadata = temporaryEdit.second!!))
        } else {
            this.add(play)
        }
    }

    private fun clearTemporaryEdit(play: Play) {
        val temporaryEdit = temporaryEdits[play.source]
        if (temporaryEdit != null && temporaryEdit.first.timestamp == play.timestamp) {
            temporaryEdits.remove(play.source)
        }
    }

    suspend fun process(snapshot: List<Event>): List<Play> {
        logger.debug { "processing ${snapshot.size} events" }
        val resultPlays = mutableListOf<Play>()

        snapshot.groupBy { it.source }.forEach { (source, events) ->
            processSingleSource(resultPlays, source, events)
        }
        return resultPlays
    }

    private suspend fun processSingleSource(resultPlays: MutableList<Play>, source: Source, events: List<Event>) {
        var play = getLastPlay(source)
        val eventsSorted = events.sortedBy { it.timestamp }
        if (play != null && eventsSorted[0].timestamp < play.timestamp) {
            logger.error {
                "ERROR: out-of-sync events source=$source " +
                        "eventTimestamp=${eventsSorted[0].timestamp} playTimestamp=${play!!.timestamp}"
            }
            return
        }

        eventsSorted.forEach { event ->
            logger.debug { "started processing $event" }
            if (play == null) {
                if (event.isPlaying) {
                    play = Play.fromEvent(event)
                }
                return@forEach
            }

            // if past the end of previous play, plug hole
            val shouldPlugHole = event.timestamp > play.endTimestamp
            if (play.lastPlaying && shouldPlugHole) {
                play.timePlayed += play.endTimestamp - play.timestamp
                play.plug(play.endTimestamp, play.duration, play.lastSpeed)
            }

            // if hole is plugged and needs to be unplugged, unplug
            if (play.isPlugged && !shouldPlugHole) {
                play.associatedEvents.removeAt(play.associatedEvents.size - 1)
                play.timePlayed -= play.endTimestamp - play.timestamp
            }

            // add time passed
            if (play.lastPlaying && !shouldPlugHole) {
                play.timePlayed += event.timestamp - play.lastTimestamp
            }

            val isNewPlay = isNewPlay(event, play)
            logger.debug { "$event $play decided $isNewPlay" }

            if (isNewPlay) {
                if (play.lastPlaying) {
                    play.plug(event.timestamp, event.position, play.lastSpeed)
                }
                resultPlays.applyTemporaryEditAndAdd(play)
                clearTemporaryEdit(play)
                play = Play.fromEvent(event)
            } else {
                play.associatedEvents.add(event.info)
            }
            logger.debug { "finished processing $event" }
        }

        if (play != null)
            resultPlays.applyTemporaryEditAndAdd(play)
    }

    private fun isNewPlay(event: Event, lastPlay: Play): Boolean {
        return if (event.isPlaying) {
            if (event.position <= SKIP_MIN_DURATION) {
                event.metadata != lastPlay.metadata ||
                        lastPlay.lastPosition > SKIP_MIN_DURATION ||
                        (event.timestamp - lastPlay.lastTimestamp) > SKIP_MIN_DURATION
            } else {
                event.metadata != lastPlay.metadata
            }
        } else {
            event.position <= SKIP_MIN_DURATION && lastPlay.lastPosition > SKIP_MIN_DURATION
        }
    }
}