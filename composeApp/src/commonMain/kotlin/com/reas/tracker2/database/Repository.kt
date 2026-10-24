package com.reas.tracker2.database

import androidx.paging.PagingSource
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import com.reas.tracker2.database.entities.*
import com.reas.tracker2.shared.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.uuid.Uuid

class Repository(
    private val db: AppDatabase,
) {
    private fun ArtistEntity.toObject() = Artist(name, artistId)

    private fun Scrobble.toEntity() =
        ScrobbleEntity(
            id = id,
            trackId = metadata.id,
            isDeleted = isDeleted,
            artists = artistsAsString,
            albumArtists = albumArtistsAsString,
            timestamp = timestamp,
            duration = duration,
            timePlayed = timePlayed,
            lastPosition = lastPosition,
            lastPlaying = lastPlaying,
            sourceDevice = client,
            sourceApp = app,
            associatedEvents = associatedEvents,
        )

    private fun ScrobbleEdit.toEntity() =
        EditEntity(
            id = id,
            scrobbleId = scrobbleId,
            trackId = track.id,
            isDeleted = isDeleted,
            timestamp = timestamp,
            reason = reason,
        )

    suspend fun <T> readTransaction(block: suspend () -> T): T = db.useReaderConnection { block() }

    suspend fun <T> writeTransaction(block: suspend () -> T): T = db.useWriterConnection { block() }

    suspend fun insertTrack(name: String, artists: ArtistList, album: Album?): Long =
        db.trackDao().insertTrack(
            TrackEntity(
                name = name,
                albumId = album?.id,
                artists = artists.raw,
                artistIds = artists.ids,
            ),
        )

    suspend fun insertAlbum(name: String, artists: ArtistList): Long =
        db.trackDao().insertAlbum(
            AlbumEntity(name = name, artists = artists.raw, artistIds = artists.ids),
        )

    suspend fun insertArtists(artists: List<String>): List<Long> = db.trackDao().insertArtists(artists.map { ArtistEntity(name = it) })

    suspend fun associateArtistsWithAlbum(albumId: Long, artists: ArtistList) =
        db.trackDao().insertAlbumArtistCrossRefs(
            artists.map { artist ->
                AlbumArtistCrossRef(albumId, artist.id)
            },
        )

    suspend fun associateArtistsWithTrack(trackId: Long, artists: ArtistList) =
        db.trackDao().insertTrackArtistCrossRefs(
            artists.map { artist ->
                TrackArtistCrossRef(trackId, artist.id)
            },
        )

    suspend fun getArtists(names: List<String>): List<Artist> = db.trackDao().getArtists(names).map { it.toObject() }

    suspend fun getAlbum(name: String, artists: ArtistList): Album? = db.trackDao().getAlbum(name, artists.ids)?.toObject()

    suspend fun getTrack(name: String, artists: ArtistList, album: Album?): Track? =
        db.trackDao().getTrack(name, artists.ids, album?.id)?.toObject()

    suspend fun getTrackById(id: Long): Track = db.trackDao().getTrackById(id).toObject()

    suspend fun insertScrobble(scrobble: Scrobble): Long = db.scrobbleDao().insert(scrobble.toEntity())

    suspend fun insertScrobbles(scrobbles: List<Scrobble>) = db.scrobbleDao().insertBatch(scrobbles.map { it.toEntity() })

    suspend fun deleteScrobble(scrobble: Scrobble) = db.scrobbleDao().delete(scrobble.toEntity())

    suspend fun updateScrobbles(scrobble: Scrobble) = db.scrobbleDao().update(scrobble.toEntity())

    suspend fun updateScrobbleEvents(scrobbles: List<Scrobble>) = db.scrobbleDao().updateAssociatedEvents(scrobbles.map { it.toEntity() })

    suspend fun getScrobble(id: Uuid): Scrobble? = db.scrobbleDao().getScrobble(id)?.toObject()

    suspend fun getLastScrobbleFromSource(source: Source): Scrobble? =
        db.scrobbleDao().getLastScrobbleFromSource(source.client, source.app)?.toObject()

    suspend fun getFirstEdit(scrobble: Scrobble): EditWithMetadata? = db.editDao().getFirstEdit(scrobble.id)

    suspend fun markScrobbleDeleted(scrobble: Scrobble) = db.scrobbleDao().markScrobbleDeleted(scrobble.id)

    fun getNowPlaying(): Flow<List<Scrobble>> = db.scrobbleDao().getNowPlaying().map { it.map { it.toObject() } }

    fun getRecentScrobbles(): PagingSource<Int, ScrobbleWithData> = db.scrobbleDao().getRecentScrobbles()

    fun getArtistPlayCount(artist: Artist, period: TimePeriod): Flow<Int> =
        db.scrobbleDao().getArtistPlayCount(artist.id, period.start, period.end)

    fun getArtistTimePlayed(artist: Artist, period: TimePeriod): Flow<Duration> =
        db.scrobbleDao().getArtistTimePlayed(artist.id, period.start, period.end)

    fun getMostPlayedArtists(period: TimePeriod): PagingSource<Int, ArtistWithTimePlayed> =
        db.scrobbleDao().getMostPlayedArtists(period.start, period.end)

    fun getMostPlayedArtists(period: TimePeriod, limit: Int): Flow<List<ArtistWithTimePlayed>> =
        db.scrobbleDao().getMostPlayedArtists(period.start, period.end, limit)

    fun getMostPlayedArtistsByPlayCount(period: TimePeriod): PagingSource<Int, ArtistWithPlayCount> =
        db.scrobbleDao().getMostPlayedArtistsByPlayCount(period.start, period.end)

    fun getArtistRank(artist: Artist, period: TimePeriod): Flow<Int> = db.scrobbleDao().getArtistRank(artist.id, period.start, period.end)

    fun getArtistRankByPlayCount(artist: Artist, period: TimePeriod): Flow<Int> =
        db.scrobbleDao().getArtistRankByPlayCount(artist.id, period.start, period.end)

    fun getAlbumPlayCount(album: Album, period: TimePeriod): Flow<Int> =
        db.scrobbleDao().getAlbumPlayCount(album.id, period.start, period.end)

    fun getAlbumTimePlayed(album: Album, period: TimePeriod): Flow<Duration> =
        db.scrobbleDao().getAlbumTimePlayed(album.id, period.start, period.end)

    fun getMostPlayedAlbums(period: TimePeriod): PagingSource<Int, AlbumWithTimePlayed> =
        db.scrobbleDao().getMostPlayedAlbums(period.start, period.end)

    fun getMostPlayedAlbums(period: TimePeriod, limit: Int): Flow<List<AlbumWithTimePlayed>> =
        db.scrobbleDao().getMostPlayedAlbums(period.start, period.end, limit)

    fun getMostPlayedAlbumsByPlayCount(period: TimePeriod): PagingSource<Int, AlbumWithPlayCount> =
        db.scrobbleDao().getMostPlayedAlbumsByPlayCount(period.start, period.end)

    fun getMostPlayedAlbumsFromArtist(artist: Artist, period: TimePeriod): PagingSource<Int, AlbumWithTimePlayed> =
        db.scrobbleDao().getMostPlayedAlbumsFromArtist(artist.id, period.start, period.end)

    fun getMostPlayedAlbumsFromArtist(artist: Artist, period: TimePeriod, limit: Int): Flow<List<AlbumWithTimePlayed>> =
        db.scrobbleDao().getMostPlayedAlbumsFromArtist(artist.id, period.start, period.end, limit)

    fun getMostPlayedAlbumsFromArtistByPlayCount(artist: Artist, period: TimePeriod): PagingSource<Int, AlbumWithPlayCount> =
        db.scrobbleDao().getMostPlayedAlbumsFromArtistByPlayCount(artist.id, period.start, period.end)

    fun getMostPlayedAlbumsFromArtistByPlayCount(artist: Artist, period: TimePeriod, limit: Int): Flow<List<AlbumWithPlayCount>> =
        db.scrobbleDao().getMostPlayedAlbumsFromArtistByPlayCount(artist.id, period.start, period.end, limit)

    fun getAlbumRank(album: Album, period: TimePeriod): Flow<Int> = db.scrobbleDao().getAlbumRank(album.id, period.start, period.end)

    fun getAlbumRankByPlayCount(album: Album, period: TimePeriod): Flow<Int> =
        db.scrobbleDao().getAlbumRankByPlayCount(album.id, period.start, period.end)

    fun getTrackPlayCount(track: Track, period: TimePeriod): Flow<Int> =
        db.scrobbleDao().getTrackPlayCount(track.id, period.start, period.end)

    fun getTrackTimePlayed(track: Track, period: TimePeriod): Flow<Duration> =
        db.scrobbleDao().getTrackTimePlayed(track.id, period.start, period.end)

    fun getTrackHistory(track: Track): PagingSource<Int, ScrobbleWithData> = db.scrobbleDao().getTrackHistory(track.id)

    fun getMostPlayedTracks(period: TimePeriod): PagingSource<Int, TrackWithTimePlayed> =
        db.scrobbleDao().getMostPlayedTracks(period.start, period.end)

    fun getMostPlayedTracks(period: TimePeriod, limit: Int): Flow<List<TrackWithTimePlayed>> =
        db.scrobbleDao().getMostPlayedTracks(period.start, period.end, limit)

    fun getMostPlayedTracksByPlayCount(period: TimePeriod): PagingSource<Int, TrackWithPlayCount> =
        db.scrobbleDao().getMostPlayedTracksByPlayCount(period.start, period.end)

    fun getMostPlayedTracksFromArtist(artist: Artist, period: TimePeriod): PagingSource<Int, TrackWithTimePlayed> =
        db.scrobbleDao().getMostPlayedTracksFromArtist(artist.id, period.start, period.end)

    fun getMostPlayedTracksFromArtistByPlayCount(artist: Artist, period: TimePeriod): PagingSource<Int, TrackWithPlayCount> =
        db.scrobbleDao().getMostPlayedTracksFromArtistByPlayCount(artist.id, period.start, period.end)

    fun getMostPlayedTracksFromAlbum(album: Album, period: TimePeriod): PagingSource<Int, TrackWithTimePlayed> =
        db.scrobbleDao().getMostPlayedTracksFromAlbum(album.id, period.start, period.end)

    fun getMostPlayedTracksFromAlbumByPlayCount(album: Album, period: TimePeriod): PagingSource<Int, TrackWithPlayCount> =
        db.scrobbleDao().getMostPlayedTracksFromAlbumByPlayCount(album.id, period.start, period.end)

    fun getMostPlayedTracksFromArtist(artist: Artist, period: TimePeriod, limit: Int): Flow<List<TrackWithTimePlayed>> =
        db.scrobbleDao().getMostPlayedTracksFromArtist(artist.id, period.start, period.end, limit)

    fun getMostPlayedTracksFromArtistByPlayCount(artist: Artist, period: TimePeriod, limit: Int): Flow<List<TrackWithPlayCount>> =
        db.scrobbleDao().getMostPlayedTracksFromArtistByPlayCount(artist.id, period.start, period.end, limit)

    fun getMostPlayedTracksFromAlbum(album: Album, period: TimePeriod, limit: Int): Flow<List<TrackWithTimePlayed>> =
        db.scrobbleDao().getMostPlayedTracksFromAlbum(album.id, period.start, period.end, limit)

    fun getMostPlayedTracksFromAlbumByPlayCount(album: Album, period: TimePeriod, limit: Int): Flow<List<TrackWithPlayCount>> =
        db.scrobbleDao().getMostPlayedTracksFromAlbumByPlayCount(album.id, period.start, period.end, limit)

    fun getTrackRank(track: Track, period: TimePeriod): Flow<Int> = db.scrobbleDao().getTrackRank(track.id, period.start, period.end)

    fun getTrackRankByPlayCount(track: Track, period: TimePeriod): Flow<Int> =
        db.scrobbleDao().getTrackRankByPlayCount(track.id, period.start, period.end)

    suspend fun insertEdit(edit: ScrobbleEdit) = db.editDao().insert(edit.toEntity())

    suspend fun updateEdit(edit: ScrobbleEdit) = db.editDao().update(edit.toEntity())

    suspend fun deleteEdit(edit: ScrobbleEdit) = db.editDao().delete(edit.toEntity())

    fun getPlayCount(): Flow<Int> = db.scrobbleDao().getPlayCount()

    fun getEditCount(): Flow<Int> = db.editDao().getEditCount()
}
