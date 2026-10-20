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

    suspend fun process(snapshot: List<Event>): Pair<List<Scrobble>, List<Scrobble>> {
        logger.debug { "processing ${snapshot.size} events" }
        val newScrobbles = mutableListOf<Scrobble>()
        val updatedScrobbles = mutableListOf<Scrobble>()

        snapshot.groupBy { it.source }.forEach { (source, events) ->
            processSingleSource(newScrobbles, updatedScrobbles, source, events)
        }
        return newScrobbles to updatedScrobbles
    }

    private suspend fun processSingleSource(
        newScrobbles: MutableList<Scrobble>,
        updatedScrobbles: MutableList<Scrobble>,
        source: Source,
        events: List<Event>,
    ) {
        var play = adapter.getLastScrobbleFromSource(source)
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
                logger.debug { "play == null" }
                if (event.isPlaying) {
                    play = Scrobble.fromEvent(event)
                    updatingPlay = false
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
                    updatedScrobbles.add(play)
                } else {
                    newScrobbles.add(play)
                }
                play = Scrobble.fromEvent(event)
                updatingPlay = false
            } else {
                play.associatedEvents.add(event.info)
            }
            logger.debug { "finished processing $event" }
        }

        if (play != null) {
            if (updatingPlay) {
                updatedScrobbles.add(play)
            } else {
                newScrobbles.add(play)
            }
        }
    }

    private fun isNewPlay(event: Event, lastScrobble: Scrobble): Boolean =
        if (event.isPlaying) {
            if (event.position <= SKIP_MIN_DURATION) {
                event.metadata != lastScrobble.metadata ||
                    lastScrobble.lastPosition > SKIP_MIN_DURATION ||
                    (event.timestamp - lastScrobble.lastTimestamp) > SKIP_MIN_DURATION
            } else {
                event.metadata != lastScrobble.metadata
            }
        } else {
            event.position <= SKIP_MIN_DURATION && lastScrobble.lastPosition > SKIP_MIN_DURATION
        }
}
