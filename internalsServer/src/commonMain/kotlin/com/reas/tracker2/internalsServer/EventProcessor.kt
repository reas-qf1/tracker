package com.reas.tracker2.internalsServer

import com.reas.tracker2.shared.Event
import com.reas.tracker2.shared.Scrobble
import com.reas.tracker2.shared.Source
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration.Companion.seconds

class EventProcessor(
    private val repository: Repository,
) {
    companion object {
        val START_GRACE_PERIOD = 2.seconds
        private val logger = KotlinLogging.logger {}
    }

    private suspend fun getLastScrobbleFromSource(source: Source): Scrobble? {
        return repository.readTransaction {
            val play = repository.getLastScrobbleFromSource(source) ?: return@readTransaction null
            val metadata = repository.getOriginalMetadata(play) ?: return@readTransaction play
            play.copy(metadata = metadata)
        }
    }

    suspend fun process(snapshot: List<Event>): Result {
        logger.debug { "processing ${snapshot.size} events" }
        val newScrobbles = mutableListOf<Scrobble>()
        val updatedScrobbles = mutableListOf<Scrobble>()
        val pluggedScrobbles = mutableListOf<Scrobble>()

        snapshot.groupBy { it.source }.forEach { (source, events) ->
            processSingleSource(newScrobbles, updatedScrobbles, pluggedScrobbles, source, events)
        }
        return Result(newScrobbles, updatedScrobbles, pluggedScrobbles)
    }

    private suspend fun processSingleSource(
        newScrobbles: MutableList<Scrobble>,
        updatedScrobbles: MutableList<Scrobble>,
        pluggedScrobbles: MutableList<Scrobble>,
        source: Source,
        events: List<Event>,
    ) {
        var play = getLastScrobbleFromSource(source)
        var state = State.UNCHANGED

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
                    state = State.NEW
                }
                return@forEach
            }

            // if past the end of previous play, plug hole
            val shouldPlugHole = event.timestamp > play.endTimestamp
            if (play.lastPlaying && shouldPlugHole) {
                play.timePlayed += play.endTimestamp - play.lastTimestamp
                play.plug(play.endTimestamp, play.duration, play.lastSpeed)
                if (state < State.PLUGGING) state = State.PLUGGING
            }

            // if hole is plugged and needs to be unplugged, unplug
            if (play.isPlugged && !shouldPlugHole) {
                play.associatedEvents.removeAt(play.associatedEvents.size - 1)
                play.timePlayed -= play.endTimestamp - play.lastTimestamp
            }

            // add time passed
            if (play.lastPlaying && !shouldPlugHole) {
                play.timePlayed += event.timestamp - play.lastTimestamp
            }

            val isNewPlay = isNewPlay(event, play)
            logger.debug { "isNewPlay: $isNewPlay" }

            if (isNewPlay) {
                if (play.lastPlaying) {
                    val position = play.lastPosition + (event.timestamp - play.lastTimestamp) * play.lastSpeed
                    play.plug(event.timestamp, position, play.lastSpeed)
                    if (state < State.PLUGGING) state = State.PLUGGING
                }
                if (state == State.PLUGGING) {
                    pluggedScrobbles.add(play)
                } else if (state == State.UPDATING) {
                    updatedScrobbles.add(play)
                } else if (state == State.NEW) {
                    newScrobbles.add(play)
                }
                play = Scrobble.fromEvent(event)
                state = State.NEW
            } else {
                play.associatedEvents.add(event.info)
                if (state < State.UPDATING) state = State.UPDATING
            }
        }

        if (play != null) {
            if (state == State.PLUGGING) {
                pluggedScrobbles.add(play)
            } else if (state == State.UPDATING) {
                updatedScrobbles.add(play)
            } else if (state == State.NEW) {
                newScrobbles.add(play)
            }
        }
    }

    private fun isNewPlay(event: Event, lastScrobble: Scrobble): Boolean =
        if (event.isPlaying) {
            if (event.position <= START_GRACE_PERIOD) {
                event.metadata != lastScrobble.metadata ||
                    lastScrobble.lastPosition > START_GRACE_PERIOD ||
                    (event.timestamp - lastScrobble.lastTimestamp) > START_GRACE_PERIOD
            } else {
                event.metadata != lastScrobble.metadata
            }
        } else {
            event.position <= START_GRACE_PERIOD && lastScrobble.lastPosition > START_GRACE_PERIOD
        }

    data class Result(
        val new: List<Scrobble>,
        val updated: List<Scrobble>,
        val plugged: List<Scrobble>,
    )

    private enum class State {
        UNCHANGED,
        PLUGGING,
        UPDATING,
        NEW,
    }
}
