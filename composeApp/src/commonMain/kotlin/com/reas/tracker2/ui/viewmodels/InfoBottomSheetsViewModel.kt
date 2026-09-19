package com.reas.tracker2.ui.viewmodels

import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.Album
import com.reas.tracker2.shared.Artist
import com.reas.tracker2.shared.TimePeriod
import com.reas.tracker2.shared.TrackWithAlbum
import com.reas.tracker2.util.toDisplayString
import kotlinx.coroutines.flow.map

class InfoBottomSheetsViewModel(
    private val repository: Repository,
) : TrackerViewModel() {
    fun artistPlays(artist: Artist) =
        repository
            .getArtistPlays(artist, TimePeriod.ALLTIME)
            .map { it.toString() }
            .asStringStateFlow()

    fun artistTimePlayed(artist: Artist) =
        repository
            .getArtistTimePlayed(artist, TimePeriod.ALLTIME)
            .map { it.toDisplayString() }
            .asStringStateFlow()

    fun trackPlays(track: TrackWithAlbum) =
        repository
            .getTrackPlays(track, TimePeriod.ALLTIME)
            .map { it.toString() }
            .asStringStateFlow()

    fun trackTimePlayed(track: TrackWithAlbum) =
        repository
            .getTrackTimePlayed(track, TimePeriod.ALLTIME)
            .map { it.toDisplayString() }
            .asStringStateFlow()

    fun albumPlays(album: Album) =
        repository
            .getAlbumPlays(album, TimePeriod.ALLTIME)
            .map { it.toString() }
            .asStringStateFlow()

    fun albumTimePlayed(album: Album) =
        repository
            .getAlbumTimePlayed(album, TimePeriod.ALLTIME)
            .map { it.toDisplayString() }
            .asStringStateFlow()
}
