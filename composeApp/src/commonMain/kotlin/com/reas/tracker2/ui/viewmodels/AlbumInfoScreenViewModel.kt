package com.reas.tracker2.ui.viewmodels

import com.reas.tracker2.database.Repository
import com.reas.tracker2.network.NetworkRepository
import com.reas.tracker2.settings.Settings
import com.reas.tracker2.settings.chartSort
import com.reas.tracker2.shared.Album
import com.reas.tracker2.shared.TimePeriod
import com.reas.tracker2.shared.TrackWithAlbum
import com.reas.tracker2.ui.components.ChartEntryUiState
import com.reas.tracker2.ui.navigation.BottomSheetInfo
import com.reas.tracker2.ui.navigation.ChartSort
import com.reas.tracker2.util.toDisplayString
import kotlinx.coroutines.flow.map
import org.koin.core.time.inMs

class AlbumInfoScreenViewModel(
    private val repository: Repository,
    private val networkRepository: NetworkRepository,
    private val settings: Settings,
): TrackerViewModel() {
    fun plays(album: Album, period: TimePeriod) =
        repository.getAlbumPlays(album, period).asIntStateFlow()

    fun timePlayed(album: Album, period: TimePeriod) =
        repository.getAlbumTimePlayed(album, period).asDurationStateFlow()

    fun rank(album: Album, period: TimePeriod) =
        repository.getAlbumRank(album, period)
            .map { "#" + (it + 1).toString() }
            .asStringStateFlow()

    fun playRank(album: Album, period: TimePeriod) =
        repository.getAlbumRankByPlayCount(album, period)
            .map { "#" + (it + 1).toString() }
            .asStringStateFlow()

    fun topTracks(album: Album, period: TimePeriod) =
        repository.getMostPlayedTracksFromAlbum(album, period, limit = 5)
            .mapElements { info ->
                ChartEntryUiState(
                    label = info.track.name,
                    label2 = if (info.track.artists.toSet() != album.artists.toSet()) info.track.artistsAsString else null,
                    key = info.toString(),
                    metric = info.timePlayed.inMs,
                    metricAsString = info.timePlayed.toDisplayString(),
                    bottomSheetInfo = BottomSheetInfo(track = info.track),
                    url = { getTrackImageUrl(info.track) }
                )
            }.asListStateFlow()

    fun topTracksByPlayCount(album: Album, period: TimePeriod) =
        repository.getMostPlayedTracksFromAlbumByPlayCount(album, period, limit = 5)
            .mapElements { info ->
                ChartEntryUiState(
                    label = info.track.name,
                    label2 = if (info.track.artists.toSet() != album.artists.toSet()) info.track.artistsAsString else null,
                    key = info.toString(),
                    metric = info.playCount.toDouble(),
                    metricAsString = "${info.playCount} plays",
                    bottomSheetInfo = BottomSheetInfo(track = info.track),
                    url = { getTrackImageUrl(info.track) }
                )
            }.asListStateFlow()

    fun sort() = settings.stateFlow(chartSort)

    suspend fun setSort(sort: ChartSort) {
        settings[chartSort] = sort
    }

    suspend fun getAlbumImageUrl(album: Album): String? {
        return networkRepository.getAlbumImageUrl(album, "large")
    }

    suspend fun getTrackImageUrl(track: TrackWithAlbum): String? {
        return track.asAlbum?.let { getAlbumImageUrl(it) }
    }
}