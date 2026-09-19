package com.reas.tracker2.shared

interface EventProcessorAdapter {
    suspend fun getLastPlayFromSource(source: Source): Play?
}
