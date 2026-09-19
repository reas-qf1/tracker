package com.reas.tracker2.util

import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.EventProcessorAdapter
import com.reas.tracker2.shared.Source

class EventProcessorAdapterImpl(
    private val repository: Repository,
) : EventProcessorAdapter {
    override suspend fun getLastPlayFromSource(source: Source) = repository.getLastPlayFromSource(source)
}
