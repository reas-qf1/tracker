package com.reas.tracker2.ui.sheets.info

import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.Album
import com.reas.tracker2.shared.Artist
import com.reas.tracker2.shared.TimePeriod
import com.reas.tracker2.shared.Track
import com.reas.tracker2.ui.TrackerViewModel
import com.reas.tracker2.util.toDisplayString
import kotlinx.coroutines.flow.map

class InfoBottomSheetsViewModel(
    private val repository: Repository,
) : TrackerViewModel() {
    fun artistPlayCount(artist: Artist) =
        repository
            .getArtistPlayCount(artist, TimePeriod.ALLTIME)
            .map { it.toString() }
            .asStringStateFlow()

    fun artistTimePlayed(artist: Artist) =
        repository
            .getArtistTimePlayed(artist, TimePeriod.ALLTIME)
            .map { it.toDisplayString() }
            .asStringStateFlow()

    fun trackPlayCount(track: Track) =
        repository
            .getTrackPlayCount(track, TimePeriod.ALLTIME)
            .map { it.toString() }
            .asStringStateFlow()

    fun trackTimePlayed(track: Track) =
        repository
            .getTrackTimePlayed(track, TimePeriod.ALLTIME)
            .map { it.toDisplayString() }
            .asStringStateFlow()

    fun albumPlayCount(album: Album) =
        repository
            .getAlbumPlayCount(album, TimePeriod.ALLTIME)
            .map { it.toString() }
            .asStringStateFlow()

    fun albumTimePlayed(album: Album) =
        repository
            .getAlbumTimePlayed(album, TimePeriod.ALLTIME)
            .map { it.toDisplayString() }
            .asStringStateFlow()
}
