package com.reas.tracker2.shared

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration.Companion.seconds

class EventProcessor(
    private val adapter: EventProcessorAdapter,
) {
    companion object {
        val SKIP_MIN_DURATION = 2.seconds
        private val logger = KotlinLogging.logger {}
    }

    suspend fun process(snapshot: List<Event>): Pair<List<Play>, List<Play>> {
        logger.debug { "processing ${snapshot.size} events" }
        val newPlays = mutableListOf<Play>()
        val updatedPlays = mutableListOf<Play>()

        snapshot.groupBy { it.source }.forEach { (source, events) ->
            processSingleSource(newPlays, updatedPlays, source, events)
        }
        return newPlays to updatedPlays
    }

    private suspend fun processSingleSource(
        newPlays: MutableList<Play>,
        updatedPlays: MutableList<Play>,
        source: Source,
        events: List<Event>,
    ) {
        var play = adapter.getLastPlayFromSource(source)
        var updatingPlay = true

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
                    val position = play.lastPosition + (event.timestamp - play.lastTimestamp) * play.lastSpeed
                    play.plug(event.timestamp, position, play.lastSpeed)
                }
                if (updatingPlay) {
                    updatedPlays.add(play)
                } else {
                    newPlays.add(play)
                }
                play = Play.fromEvent(event)
                updatingPlay = false
            } else {
                play.associatedEvents.add(event.info)
            }
            logger.debug { "finished processing $event" }
        }

        if (play != null) {
            if (updatingPlay) {
                updatedPlays.add(play)
            } else {
                newPlays.add(play)
            }
        }
    }

    private fun isNewPlay(event: Event, lastPlay: Play): Boolean =
        if (event.isPlaying) {
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
