package com.reas.tracker2.util

import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.EventProcessorAdapter
import com.reas.tracker2.shared.Scrobble
import com.reas.tracker2.shared.Source

class EventProcessorAdapterImpl(
    private val repository: Repository,
) : EventProcessorAdapter {
    override suspend fun getLastScrobbleFromSource(source: Source): Scrobble? {
        val play = repository.getLastScrobbleFromSource(source) ?: return null
        val metadata = repository.getOriginalMetadata(play) ?: return play
        return play.copy(metadata = metadata)
    }
}
