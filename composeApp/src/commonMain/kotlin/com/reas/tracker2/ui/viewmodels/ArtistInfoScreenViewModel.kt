package com.reas.tracker2.ui.viewmodels

import com.reas.tracker2.database.Repository
import com.reas.tracker2.network.NetworkRepository
import com.reas.tracker2.settings.Settings
import com.reas.tracker2.settings.chartSort
import com.reas.tracker2.shared.Album
import com.reas.tracker2.shared.Artist
import com.reas.tracker2.shared.TimePeriod
import com.reas.tracker2.shared.TrackWithAlbum
import com.reas.tracker2.ui.components.ChartEntryUiState
import com.reas.tracker2.ui.navigation.BottomSheetInfo
import com.reas.tracker2.ui.navigation.ChartSort
import com.reas.tracker2.util.toDisplayString
import kotlinx.coroutines.flow.map
import org.koin.core.time.inMs

class ArtistInfoScreenViewModel(
    private val repository: Repository,
    private val settings: Settings,
    private val networkRepository: NetworkRepository,
): TrackerViewModel() {
    fun plays(artist: Artist, period: TimePeriod) =
        repository.getArtistPlays(artist, period).asIntStateFlow()

    fun timePlayed(artist: Artist, period: TimePeriod) =
        repository.getArtistTimePlayed(artist, period).asDurationStateFlow()

    fun rank(artist: Artist, period: TimePeriod) =
        repository.getArtistRank(artist, period)
            .map { "#" + (it + 1).toString() }
            .asStringStateFlow()

    fun playRank(artist: Artist, period: TimePeriod) =
        repository.getArtistRankByPlayCount(artist, period)
            .map { "#" + (it + 1).toString() }
            .asStringStateFlow()

    fun topAlbums(artist: Artist, period: TimePeriod) =
        repository.getMostPlayedAlbumsFromArtist(artist, period, limit = 5)
            .mapElements { info ->
                ChartEntryUiState(
                    label = info.album.name,
                    label2 = if (info.album.artists.size > 1) info.album.artistsAsString else null,
                    key = info.toString(),
                    metric = info.timePlayed.inMs,
                    metricAsString = info.timePlayed.toDisplayString(),
                    bottomSheetInfo = BottomSheetInfo(album = info.album),
                    url = { getAlbumImageUrl(info.album) }
                )
            }.asListStateFlow()

    fun topAlbumsByPlayCount(artist: Artist, period: TimePeriod) =
        repository.getMostPlayedAlbumsFromArtistByPlayCount(artist, period, limit = 5)
            .mapElements { info ->
                ChartEntryUiState(
                    label = info.album.name,
                    label2 = if (info.album.artists.size > 1) info.album.artistsAsString else null,
                    key = info.toString(),
                    metric = info.playCount.toDouble(),
                    metricAsString = "${info.playCount} plays",
                    bottomSheetInfo = BottomSheetInfo(album = info.album),
                    url = { getAlbumImageUrl(info.album) }
                )
            }.asListStateFlow()

    fun topTracks(artist: Artist, period: TimePeriod) =
        repository.getMostPlayedTracksFromArtist(artist, period, limit = 5)
            .mapElements { info ->
                ChartEntryUiState(
                    label = info.track.name,
                    label2 = if (info.track.artists.size > 1) info.track.artistsAsString else null,
                    key = info.toString(),
                    metric = info.timePlayed.inMs,
                    metricAsString = info.timePlayed.toDisplayString(),
                    bottomSheetInfo = BottomSheetInfo(track = info.track),
                    url = { getTrackImageUrl(info.track) }
                )
            }.asListStateFlow()

    fun topTracksByPlayCount(artist: Artist, period: TimePeriod) =
        repository.getMostPlayedTracksFromArtistByPlayCount(artist, period, limit = 5)
            .mapElements { info ->
                ChartEntryUiState(
                    label = info.track.name,
                    label2 = if (info.track.artists.size > 1) info.track.artistsAsString else null,
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