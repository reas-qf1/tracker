package com.reas.tracker2.shared

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.time.Clock

class HolePlugger(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val clock: Clock = Clock.System,
) {
    private val plugJobs = mutableMapOf<String, Job>()
    private val scrobbleFlow = MutableSharedFlow<Scrobble>()

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
            scrobbleFlow.emit(scrobble)
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

    suspend fun collectPlays(block: suspend (Scrobble) -> Unit) {
        scrobbleFlow.collect(block)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
