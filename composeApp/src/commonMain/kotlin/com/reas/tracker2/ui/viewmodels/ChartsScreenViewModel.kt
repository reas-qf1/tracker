package com.reas.tracker2.ui.viewmodels

import androidx.paging.PagingData
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
import com.reas.tracker2.ui.navigation.ChartType
import com.reas.tracker2.ui.navigation.Charts
import com.reas.tracker2.util.toDisplayString
import kotlinx.coroutines.flow.Flow
import org.koin.core.time.inMs

class ChartsScreenViewModel(
    private val repository: Repository,
    private val networkRepository: NetworkRepository,
    private val settings: Settings,
) : TrackerViewModel() {
    private fun artists(period: TimePeriod) = pagingDataFlow {
        repository.getMostPlayedArtists(period)
    }.mapElements { info ->
        ChartEntryUiState(
            label = info.artist.name,
            label2 = null,
            key = info.artist.name,
            metric = info.timePlayed.inMs,
            metricAsString = info.timePlayed.toDisplayString(),
            bottomSheetInfo = BottomSheetInfo(artist = info.artist),
            url = { getArtistImageUrl(info.artist) }
        )
    }

    private fun albums(period: TimePeriod) = pagingDataFlow {
        repository.getMostPlayedAlbums(period)
    }.mapElements { info ->
        ChartEntryUiState(
            label = info.album.name,
            label2 = info.album.artistsAsString,
            key = info.album.toString(),
            metric = info.timePlayed.inMs,
            metricAsString = info.timePlayed.toDisplayString(),
            bottomSheetInfo = BottomSheetInfo(album = info.album),
            url = { getAlbumImageUrl(info.album) }
        )
    }

    private fun tracks(period: TimePeriod) = pagingDataFlow {
        repository.getMostPlayedTracks(period)
    }.mapElements { info ->
        ChartEntryUiState(
            label = info.track.name,
            label2 = info.track.artistsAsString,
            key = info.track.toString(),
            metric = info.timePlayed.inMs,
            metricAsString = info.timePlayed.toDisplayString(),
            bottomSheetInfo = BottomSheetInfo(track = info.track),
            url = { getTrackImageUrl(info.track) }
        )
    }

    private fun artistsByPlayCount(period: TimePeriod) = pagingDataFlow {
        repository.getMostPlayedArtistsByPlayCount(period)
    }.mapElements { info ->
        ChartEntryUiState(
            label = info.artist.name,
            label2 = null,
            key = info.artist.name,
            metric = info.playCount.toDouble(),
            metricAsString = "${info.playCount} plays",
            bottomSheetInfo = BottomSheetInfo(artist = info.artist),
            url = { getArtistImageUrl(info.artist) }
        )
    }

    private fun albumsByPlayCount(period: TimePeriod) = pagingDataFlow {
        repository.getMostPlayedAlbumsByPlayCount(period)
    }.mapElements { info ->
        ChartEntryUiState(
            label = info.album.name,
            label2 = info.album.artistsAsString,
            key = info.album.toString(),
            metric = info.playCount.toDouble(),
            metricAsString = "${info.playCount} plays",
            bottomSheetInfo = BottomSheetInfo(album = info.album),
            url = { getAlbumImageUrl(info.album) }
        )
    }

    private fun tracksByPlayCount(period: TimePeriod) = pagingDataFlow {
        repository.getMostPlayedTracksByPlayCount(period)
    }.mapElements { info ->
        ChartEntryUiState(
            label = info.track.name,
            label2 = info.track.artistsAsString,
            key = info.track.toString(),
            metric = info.playCount.toDouble(),
            metricAsString = "${info.playCount} plays",
            bottomSheetInfo = BottomSheetInfo(track = info.track),
            url = { getTrackImageUrl(info.track) }
        )
    }

    fun getInfo(arguments: Charts, sort: ChartSort): Flow<PagingData<ChartEntryUiState>> {
        val type = arguments.type
        val period = TimePeriod.ALLTIME

        return when {
            (type == ChartType.ARTISTS && sort == ChartSort.TIME) ->
                artists(period)
            (type == ChartType.ALBUMS && sort == ChartSort.TIME) ->
                albums(period)
            (type == ChartType.TRACKS && sort == ChartSort.TIME) ->
                tracks(period)
            (type == ChartType.ARTISTS && sort == ChartSort.PLAYS) ->
                artistsByPlayCount(period)
            (type == ChartType.ALBUMS && sort == ChartSort.PLAYS) ->
                albumsByPlayCount(period)
            (type == ChartType.TRACKS && sort == ChartSort.PLAYS) ->
                tracksByPlayCount(period)

            else -> throw IllegalArgumentException("unreachable")
        }
    }

    suspend fun getArtistImageUrl(artist: Artist) = null

    suspend fun getAlbumImageUrl(album: Album): String? {
        return networkRepository.getAlbumImageUrl(album, "large")
    }

    suspend fun getTrackImageUrl(track: TrackWithAlbum): String? {
        return track.asAlbum?.let { getAlbumImageUrl(it) }
    }

    fun sort() = settings.stateFlow(chartSort)

    suspend fun setSort(sort: ChartSort) {
        settings[chartSort] = sort
    }
}