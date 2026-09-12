package com.reas.tracker2.ui.viewmodels

import com.reas.tracker2.database.Repository
import com.reas.tracker2.network.NetworkRepository
import com.reas.tracker2.shared.EventProcessor
import com.reas.tracker2.shared.Play
import com.reas.tracker2.shared.TimePeriod
import com.reas.tracker2.shared.TrackWithAlbum
import kotlinx.coroutines.flow.map

class TrackHistoryViewModel(
    private val repository: Repository,
    private val networkRepository: NetworkRepository,
    private val eventProcessor: EventProcessor
): TrackerViewModel() {
    fun history(track: TrackWithAlbum) =
        pagingDataFlow { repository.getTrackHistory(track) }
            .mapElements { entity -> repository.playEntityToObject(entity) }

    fun trackPlays(track: TrackWithAlbum) =
        repository.getTrackPlays(track, TimePeriod.ALLTIME)
            .map { it.toString() }
            .asStringStateFlow()

    suspend fun getImageUrl(scrobble: Play): String? {
        scrobble.asAlbum?.let { album ->
            return networkRepository.getAlbumImageUrl(album, "large")
        }
        return null
    }

    suspend fun delete(scrobble: Play) {
        eventProcessor.addTemporaryEdit(scrobble, null)
        repository.deletePlay(scrobble)
    }

    suspend fun edit(scrobble: Play, newMetadata: TrackWithAlbum) {
        eventProcessor.addTemporaryEdit(scrobble, newMetadata)
        repository.updatePlay(scrobble.copy(metadata = newMetadata))
    }
}