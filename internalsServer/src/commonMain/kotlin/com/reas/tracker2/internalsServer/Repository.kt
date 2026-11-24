package com.reas.tracker2.internalsServer

import com.reas.tracker2.shared.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface Repository {
    // transactions
    suspend fun <T> readTransaction(block: suspend () -> T): T

    suspend fun <T> writeTransaction(block: suspend () -> T): T

    // operations for working with & registering raw metadata
    suspend fun getArtists(names: List<String>): List<Artist>

    suspend fun getAlbum(name: String, artists: ArtistList): Album?

    suspend fun getTrack(name: String, artists: ArtistList, album: Album?): Track?

    suspend fun insertTrack(name: String, artists: ArtistList, album: Album?): Long

    suspend fun insertAlbum(name: String, artists: ArtistList): Long

    suspend fun insertArtists(artists: List<String>): List<Long>

    suspend fun associateArtistsWithAlbum(albumId: Long, artists: ArtistList)

    suspend fun associateArtistsWithTrack(trackId: Long, artists: ArtistList)

    // scrobbles
    suspend fun getScrobble(id: Uuid): Scrobble?

    suspend fun insertScrobble(scrobble: Scrobble)

    suspend fun insertScrobbles(scrobbles: List<Scrobble>)

    suspend fun updateScrobbleEvents(scrobbles: List<Scrobble>)

    suspend fun getNowPlaying(): List<Scrobble>

    suspend fun getScrobbleBefore(source: Source, timestamp: Instant): Scrobble?

    suspend fun getScrobbleAfter(source: Source, timestamp: Instant): Scrobble?

    // edits
    suspend fun insertEdit(edit: ScrobbleEdit)

    suspend fun insertEdits(edits: List<ScrobbleEdit>)

    suspend fun getOriginalMetadata(scrobble: Scrobble): Track?
}
