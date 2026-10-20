package com.reas.tracker2.shared

interface EventProcessorAdapter {
    suspend fun getLastScrobbleFromSource(source: Source): Scrobble?
}
