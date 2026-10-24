package com.reas.tracker2.shared

import kotlinx.coroutines.CoroutineScope
import kotlin.time.Clock

interface HolePluggerAdapter {
    fun getScope(): CoroutineScope

    fun getClock(): Clock

    suspend fun emitScrobble(scrobble: Scrobble)
}
