package com.reas.tracker2

import com.reas.tracker2.shared.EventProcessorAdapter
import com.reas.tracker2.shared.Scrobble
import com.reas.tracker2.shared.Source
import kotlin.time.Instant

class TestEventProcessorAdapter : EventProcessorAdapter {
    override suspend fun getLastScrobbleFromSource(source: Source): Scrobble? = null

    override suspend fun clearQueue(source: Source, timestamp: Instant) {}

    override suspend fun getNextId(user: String): Long? = null
}
