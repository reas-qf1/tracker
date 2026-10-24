package com.reas.tracker2.shared

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HolePlugger(
    private val holePluggerAdapter: HolePluggerAdapter,
) {
    private val scope = holePluggerAdapter.getScope()
    private val clock = holePluggerAdapter.getClock()
    private val plugJobs = mutableMapOf<String, Job>()

    fun register(scrobble: Scrobble) {
        if (!scrobble.lastPlaying) {
            return
        }
        val key = scrobble.key
        if (plugJobs.containsKey(key)) {
            cancel(scrobble)
        }
        val delayTime = scrobble.endTimestamp - clock.now()
        logger.debug { "launching job to plug hole for $key in $delayTime" }
        plugJobs[key] = scope.launch {
            delay(delayTime)
            logger.debug { "plugging hole for $key" }
            plugJobs.remove(key) // so that no one can cancel us
            scrobble.timePlayed += scrobble.endTimestamp - scrobble.lastTimestamp
            scrobble.plug(scrobble.endTimestamp, scrobble.duration, scrobble.lastSpeed)
            holePluggerAdapter.emitScrobble(scrobble)
        }
    }

    fun register(scrobbles: List<Scrobble>) {
        scrobbles.forEach { play ->
            register(play)
        }
    }

    fun cancel(scrobble: Scrobble) {
        val key = scrobble.key
        if (plugJobs.containsKey(key)) {
            logger.debug { "cancelling job to plug hole for $key" }
            plugJobs[key]!!.cancel()
            plugJobs.remove(key)
        }
    }

    fun cancelAll() {
        plugJobs.values.forEach { it.cancel() }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
