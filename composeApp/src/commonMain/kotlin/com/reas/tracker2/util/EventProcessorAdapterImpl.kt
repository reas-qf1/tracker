package com.reas.tracker2.util

import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.EventProcessorAdapter
import com.reas.tracker2.shared.Scrobble
import com.reas.tracker2.shared.Source

class EventProcessorAdapterImpl(
    private val repository: Repository,
) : EventProcessorAdapter {
    override suspend fun getLastScrobbleFromSource(source: Source): Scrobble? {
        return repository.readTransaction {
            val play = repository.getLastScrobbleFromSource(source) ?: return@readTransaction null
            val metadata = repository.getFirstEdit(play)?.metadata?.toObject() ?: return@readTransaction play
            play.copy(metadata = metadata)
        }
    }
}
