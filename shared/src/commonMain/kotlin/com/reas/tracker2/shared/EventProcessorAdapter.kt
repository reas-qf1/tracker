package com.reas.tracker2.shared

interface EventProcessorAdapter {
    suspend fun getLastPlayFromSource(source: Source): Play?

    suspend fun getOriginalMetadata(play: Play): TrackWithAlbum?
}
