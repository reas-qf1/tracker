package com.reas.tracker2.internalsServer

import com.reas.tracker2.shared.Event
import com.reas.tracker2.shared.Scrobble
import com.reas.tracker2.shared.Source
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class EventProcessor(
    private val repository: Repository,
) {
    companion object {
        val START_GRACE_PERIOD = 2.seconds
        private val logger = KotlinLogging.logger {}
    }

    private suspend fun getScrobbleBefore(source: Source, timestamp: Instant): Scrobble? {
        return repository.readTransaction {
            val play = repository.getScrobbleBefore(source, timestamp) ?: return@readTransaction null
            val metadata = repository.getOriginalMetadata(play) ?: return@readTransaction play
            play.copy(metadata = metadata)
        }
    }

    private suspend fun getScrobbleAfter(source: Source, timestamp: Instant): Scrobble? {
        return repository.readTransaction {
            val play = repository.getScrobbleAfter(source, timestamp) ?: return@readTransaction null
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
        logger.debug { "processSingleSource($source): ${events.size} events" }
        var eventsSorted = events.sortedBy { it.timestamp }
        while (eventsSorted.isNotEmpty()) {
            val firstTimestamp = eventsSorted.first().timestamp
            val prevScrobble = getScrobbleBefore(source, firstTimestamp)
            val nextScrobble = getScrobbleAfter(source, firstTimestamp)
            val lastIndex = nextScrobble?.let { next ->
                val i = eventsSorted.indexOfFirst { it.timestamp > next.timestamp }
                if (i == -1) eventsSorted.size else i
            } ?: eventsSorted.size
            processBlock(
                newScrobbles,
                updatedScrobbles,
                pluggedScrobbles,
                prevScrobble,
                nextScrobble,
                eventsSorted.take(lastIndex),
            )
            eventsSorted = eventsSorted.drop(lastIndex)
        }
    }

    private fun processBlock(
        newScrobbles: MutableList<Scrobble>,
        updatedScrobbles: MutableList<Scrobble>,
        pluggedScrobbles: MutableList<Scrobble>,
        prevScrobble: Scrobble?,
        nextScrobble: Scrobble?,
        events: List<Event>,
    ) {
        logger.debug { "processBlock($prevScrobble, $nextScrobble): ${events.size} events" }
        var scrobble = prevScrobble
        var state = State.UNCHANGED

        var eventsS = events
        if (prevScrobble != null && prevScrobble.lastTimestamp > events.first().timestamp) {
            val lastIndex = prevScrobble.associatedEvents.indexOfFirst { it.timestamp > events.first().timestamp }
            eventsS = (
                events + prevScrobble.associatedEvents.drop(lastIndex).map {
                    Event(
                        metadata = prevScrobble.metadata,
                        duration = prevScrobble.duration,
                        info = it,
                        source = prevScrobble.source,
                    )
                }
            ).sortedBy { it.timestamp }
            repeat(prevScrobble.associatedEvents.size - lastIndex) {
                val removedEvent = prevScrobble.associatedEvents.removeAt(prevScrobble.associatedEvents.lastIndex)
                if (prevScrobble.lastPlaying) {
                    prevScrobble.timePlayed -= removedEvent.timestamp - prevScrobble.lastTimestamp
                }
            }
        }
        if (nextScrobble != null) {
            eventsS = events + listOf(
                Event(
                    metadata = nextScrobble.metadata,
                    duration = nextScrobble.duration,
                    info = nextScrobble.associatedEvents.first(),
                    source = nextScrobble.source,
                ),
            )
        }

        eventsS.forEach { event ->
            logger.debug { "started processing $event" }
            if (scrobble == null) {
                logger.debug { "play == null" }
                scrobble = Scrobble.fromEvent(event)
                state = State.NEW
                return@forEach
            }

            // if past the end of previous play, plug hole
            val shouldPlugHole = event.timestamp > scrobble.endTimestamp
            if (scrobble.lastPlaying && shouldPlugHole) {
                scrobble.timePlayed += scrobble.endTimestamp - scrobble.lastTimestamp
                scrobble.plug(scrobble.endTimestamp, scrobble.duration, scrobble.lastSpeed)
                if (state < State.PLUGGING) state = State.PLUGGING
            }

            // if hole is plugged and needs to be unplugged, unplug
            if (scrobble.isPlugged && !shouldPlugHole) {
                scrobble.associatedEvents.removeAt(scrobble.associatedEvents.size - 1)
                scrobble.timePlayed -= scrobble.endTimestamp - scrobble.lastTimestamp
            }

            // add time passed
            if (scrobble.lastPlaying && !shouldPlugHole) {
                scrobble.timePlayed += event.timestamp - scrobble.lastTimestamp
            }

            val isNewPlay = isNewPlay(event, scrobble)
            logger.debug { "isNewPlay: $isNewPlay" }

            if (isNewPlay) {
                if (scrobble.lastPlaying) {
                    val position = scrobble.lastPosition + (event.timestamp - scrobble.lastTimestamp) * scrobble.lastSpeed
                    scrobble.plug(event.timestamp, position, scrobble.lastSpeed)
                    if (state < State.PLUGGING) state = State.PLUGGING
                }
                if (state == State.PLUGGING) {
                    pluggedScrobbles.add(scrobble)
                } else if (state == State.UPDATING) {
                    updatedScrobbles.add(scrobble)
                } else if (state == State.NEW) {
                    newScrobbles.add(scrobble)
                }
                scrobble = Scrobble.fromEvent(event)
                state = State.NEW
            } else {
                scrobble.associatedEvents.add(event.info)
                if (state < State.UPDATING) state = State.UPDATING
            }
        }

        if (scrobble == null) {
            return
        }
        if (nextScrobble != null) {
            if (scrobble.associatedEvents.size == 1) {
                return
            }
            scrobble = scrobble.copy(
                id = nextScrobble.id,
                timePlayed = scrobble.timePlayed + nextScrobble.timePlayed,
                associatedEvents = (scrobble.associatedEvents.dropLast(1) + nextScrobble.associatedEvents).toMutableList(),
            )
            updatedScrobbles.add(scrobble)
        } else {
            if (state == State.PLUGGING) {
                pluggedScrobbles.add(scrobble)
            } else if (state == State.UPDATING) {
                updatedScrobbles.add(scrobble)
            } else if (state == State.NEW) {
                newScrobbles.add(scrobble)
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
