package com.reas.tracker2.ui.screens.trackhistory

import com.reas.tracker2.database.Repository
import com.reas.tracker2.network.NetworkRepository
import com.reas.tracker2.shared.Scrobble
import com.reas.tracker2.shared.TimePeriod
import com.reas.tracker2.shared.Track
import com.reas.tracker2.ui.TrackerViewModel
import kotlinx.coroutines.flow.map

class TrackHistoryViewModel(
    private val repository: Repository,
    private val networkRepository: NetworkRepository,
) : TrackerViewModel() {
    fun history(track: Track) =
        pagingDataFlow { repository.getTrackHistory(track) }
            .mapElements { entity -> entity.toObject() }

    fun playCount(track: Track) =
        repository
            .getTrackPlayCount(track, TimePeriod.ALLTIME)
            .map { it.toString() }
            .asStringStateFlow()

    suspend fun getImageUrl(scrobble: Scrobble): String? {
        scrobble.asAlbum?.let { album ->
            return networkRepository.getAlbumImageUrl(album, "large")
        }
        return null
    }
}
