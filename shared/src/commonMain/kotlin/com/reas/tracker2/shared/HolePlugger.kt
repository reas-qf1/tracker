package com.reas.tracker2.shared

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.time.Clock

class HolePlugger(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val clock: Clock = Clock.System
) {

    private val plugJobs = mutableMapOf<String, Job>()
    private val playFlow = MutableSharedFlow<Play>()

    fun register(play: Play) {
        if (!play.lastPlaying)
            return
        val key = play.key
        if (plugJobs.containsKey(key))
            cancel(play)
        val delayTime = play.duration - play.lastPosition - (clock.now() - play.lastTimestamp)
        logger.debug { "launching job to plug hole for $key in $delayTime" }
        plugJobs[key] = scope.launch {
            delay(delayTime)
            logger.debug { "plugging hole for $key" }
            plugJobs.remove(key) // so that no one can cancel us
            play.timePlayed += play.endTimestamp - play.timestamp
            play.plug(play.endTimestamp, play.duration, play.lastSpeed)
            playFlow.emit(play)
        }
    }

    fun register(plays: List<Play>) {
        plays.forEach { play ->
            register(play)
        }
    }

    fun cancel(play: Play) {
        val key = play.key
        if (plugJobs.containsKey(key)) {
            logger.debug { "cancelling job to plug hole for $key" }
            plugJobs[key]!!.cancel()
            plugJobs.remove(key)
        }
    }

    fun cancelAll() {
        plugJobs.values.forEach { it.cancel() }
    }

    suspend fun collectPlays(block: suspend (Play) -> Unit) {
        playFlow.collect(block)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}