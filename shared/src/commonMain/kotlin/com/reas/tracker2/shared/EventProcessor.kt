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

    suspend fun addTemporaryEdit(play: Play, newMetadata: TrackWithAlbum?) {
        val lastPlay = adapter.getLastPlayFromSource(play.source)
        if (lastPlay != null && lastPlay.timestamp == play.timestamp) {
            logger.debug { "addTemporaryEdit $play $newMetadata" }
            val originalPlay = temporaryEdits[play.source]?.first ?: play
            temporaryEdits[play.source] = originalPlay to newMetadata
        }
    }

    suspend fun process(snapshot: List<Event>): List<Play> {
        if (snapshot.isEmpty()) return listOf()

        logger.debug { "processing ${snapshot.size} events" }
        val resultPlays = mutableListOf<Play>()

        snapshot.groupBy { it.source }.forEach { (source, events) ->
            resultPlays.addAll(processSingleSource(source, events))
        }
        return resultPlays
    }

    private suspend fun processSingleSource(source: Source, events: List<Event>): List<Play> {
        val resultPlays = mutableListOf<Play>()
        var temporaryEdit = temporaryEdits[source]
        var processingDeleted = temporaryEdit != null && temporaryEdit.second == null

        var play = if (processingDeleted) temporaryEdit!!.first else adapter.getLastPlayFromSource(source)
        val eventsSorted = events.sortedBy { it.timestamp }
        if (play != null && eventsSorted[0].timestamp < play.timestamp) {
            logger.error {
                "ERROR: out-of-sync events source=$source " +
                        "eventTimestamp=${eventsSorted[0].timestamp} playTimestamp=${play!!.timestamp}"
            }
            return emptyList()
        }

        eventsSorted.forEach { event ->
            logger.debug { "started processing $event" }
            if (play == null) {
                if (event.isPlaying) {
                    play = Play.fromEvent(event)
                }
                return@forEach
            }

            // check if need to plug hole
            val shouldPlugHole = event.timestamp > play.endTimestamp
            if (play.lastPlaying && shouldPlugHole) {
                play.timePlayed += play.duration - play.lastPosition
                play.associatedEvents.add(EventInfo(
                    position = play.duration,
                    timestamp = play.endTimestamp,
                    speed = play.lastSpeed,
                    state = EventState.PLUGGED
                ))
            }
            if (play.associatedEvents.last().state == EventState.PLUGGED && !shouldPlugHole) {
                play.associatedEvents.removeAt(play.associatedEvents.size - 1)
                play.timePlayed -= play.duration - play.lastPosition
            }
            if (play.lastPlaying && !shouldPlugHole) {
                play.timePlayed += event.timestamp - play.lastTimestamp
            }

            val eventMetadata = temporaryEdit?.second ?: event.metadata
            val isNewPlay = if (event.isPlaying) {
                if (event.position <= SKIP_MIN_DURATION) {
                    eventMetadata != play.metadata ||
                            play.lastPosition > SKIP_MIN_DURATION ||
                            (event.timestamp - play.lastTimestamp) > SKIP_MIN_DURATION
                } else {
                    eventMetadata != play.metadata
                }
            } else {
                event.position <= SKIP_MIN_DURATION && play.lastPosition > SKIP_MIN_DURATION
            }

            logger.debug { "$event $play decided $isNewPlay" }
            if (isNewPlay) {
                if (play.lastPlaying) {
                    val eventInfo = EventInfo(
                        timestamp = event.timestamp,
                        position = play.lastPosition + (event.timestamp - play.lastTimestamp),
                        speed = play.lastSpeed,
                        state = EventState.PLUGGED
                    )
                    logger.debug { "plugging $eventInfo" }
                    play.associatedEvents.add(eventInfo)
                }
                if (!processingDeleted)
                    resultPlays.add(play)
                temporaryEdit?.let {
                    temporaryEdits.remove(source)
                    temporaryEdit = null
                    processingDeleted = false
                }
                play = Play.fromEvent(event)
            } else {
                play.associatedEvents.add(event.info)
            }
            logger.debug { "finished processing $event" }
        }

        if (play != null && !processingDeleted)
            resultPlays.add(play)
        return resultPlays
    }
}