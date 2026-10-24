package com.reas.tracker2.util

import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.HolePluggerAdapter
import com.reas.tracker2.shared.Scrobble
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Clock

class HolePluggerAdapterImpl(
    private val repository: Repository,
) : HolePluggerAdapter {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun getScope() = scope

    override fun getClock() = Clock.System

    override suspend fun emitScrobble(scrobble: Scrobble) {
        repository.insertScrobble(scrobble)
    }
}
