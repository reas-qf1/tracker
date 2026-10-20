package com.reas.tracker2.ui.screens.trackinfo

import com.reas.tracker2.database.Repository
import com.reas.tracker2.network.NetworkRepository
import com.reas.tracker2.settings.Settings
import com.reas.tracker2.settings.chartSort
import com.reas.tracker2.shared.Album
import com.reas.tracker2.shared.TimePeriod
import com.reas.tracker2.shared.Track
import com.reas.tracker2.ui.TrackerViewModel
import com.reas.tracker2.ui.navigation.ChartSort
import kotlinx.coroutines.flow.map

class TrackInfoScreenViewModel(
    private val repository: Repository,
    private val settings: Settings,
    private val networkRepository: NetworkRepository,
) : TrackerViewModel() {
    fun playCount(track: Track, period: TimePeriod) = repository.getTrackPlayCount(track, period).asIntStateFlow()

    fun timePlayed(track: Track, period: TimePeriod) = repository.getTrackTimePlayed(track, period).asDurationStateFlow()

    fun rank(track: Track, period: TimePeriod) =
        repository
            .getTrackRank(track, period)
            .map { "#" + (it + 1).toString() }
            .asStringStateFlow()

    fun rankByPlayCount(track: Track, period: TimePeriod) =
        repository
            .getTrackRankByPlayCount(track, period)
            .map { "#" + (it + 1).toString() }
            .asStringStateFlow()

    fun sort() = settings.stateFlow(chartSort)

    suspend fun setSort(sort: ChartSort) {
        settings[chartSort] = sort
    }

    suspend fun getAlbumImageUrl(album: Album): String? = networkRepository.getAlbumImageUrl(album, "large")

    suspend fun getTrackImageUrl(track: Track): String? = track.asAlbum?.let { getAlbumImageUrl(it) }
}
