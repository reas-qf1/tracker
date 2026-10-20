package com.reas.tracker2.database

import androidx.paging.PagingSource
import com.reas.tracker2.database.entities.*
import com.reas.tracker2.shared.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.uuid.Uuid

interface Repository {
    suspend fun getArtist(id: Long): Artist

    suspend fun getArtists(ids: List<Long>): List<Artist>

    suspend fun getAlbum(id: Long): Album

    suspend fun getTrack(id: Long): Track

    suspend fun getOrInsertTrack(track: Track): Long

    suspend fun insertScrobble(scrobble: Scrobble): Long

    suspend fun insertScrobbles(scrobbles: List<Scrobble>)

    suspend fun updateScrobbles(scrobble: Scrobble)

    suspend fun updateScrobbleEvents(scrobbles: List<Scrobble>)

    suspend fun deleteScrobble(scrobble: Scrobble)

    suspend fun getScrobble(id: Uuid): Scrobble?

    suspend fun getLastScrobbleFromSource(source: Source): Scrobble?

    suspend fun getOriginalMetadata(scrobble: Scrobble): Track?

    suspend fun markScrobbleDeleted(scrobble: Scrobble)

    fun getNowPlaying(): Flow<List<Scrobble>>

    fun getRecentScrobbles(): PagingSource<Int, ScrobbleWithData>

    fun getArtistPlayCount(artist: Artist, period: TimePeriod): Flow<Int>

    fun getArtistTimePlayed(artist: Artist, period: TimePeriod): Flow<Duration>

    fun getMostPlayedArtists(period: TimePeriod): PagingSource<Int, ArtistWithTimePlayed>

    fun getMostPlayedArtists(period: TimePeriod, limit: Int): Flow<List<ArtistWithTimePlayed>>

    fun getMostPlayedArtistsByPlayCount(period: TimePeriod): PagingSource<Int, ArtistWithPlayCount>

    fun getArtistRank(artist: Artist, period: TimePeriod): Flow<Int>

    fun getArtistRankByPlayCount(artist: Artist, period: TimePeriod): Flow<Int>

    fun getTrackPlayCount(track: Track, period: TimePeriod): Flow<Int>

    fun getTrackTimePlayed(track: Track, period: TimePeriod): Flow<Duration>

    fun getTrackHistory(track: Track): PagingSource<Int, ScrobbleWithData>

    fun getMostPlayedTracks(period: TimePeriod): PagingSource<Int, TrackWithTimePlayed>

    fun getMostPlayedTracks(period: TimePeriod, limit: Int): Flow<List<TrackWithTimePlayed>>

    fun getMostPlayedTracksByPlayCount(period: TimePeriod): PagingSource<Int, TrackWithPlayCount>

    fun getMostPlayedTracksFromArtist(artist: Artist, period: TimePeriod): PagingSource<Int, TrackWithTimePlayed>

    fun getMostPlayedTracksFromArtist(artist: Artist, period: TimePeriod, limit: Int): Flow<List<TrackWithTimePlayed>>

    fun getMostPlayedTracksFromArtistByPlayCount(artist: Artist, period: TimePeriod): PagingSource<Int, TrackWithPlayCount>

    fun getMostPlayedTracksFromArtistByPlayCount(artist: Artist, period: TimePeriod, limit: Int): Flow<List<TrackWithPlayCount>>

    fun getMostPlayedTracksFromAlbum(album: Album, period: TimePeriod): PagingSource<Int, TrackWithTimePlayed>

    fun getMostPlayedTracksFromAlbum(album: Album, period: TimePeriod, limit: Int): Flow<List<TrackWithTimePlayed>>

    fun getMostPlayedTracksFromAlbumByPlayCount(album: Album, period: TimePeriod): PagingSource<Int, TrackWithPlayCount>

    fun getMostPlayedTracksFromAlbumByPlayCount(album: Album, period: TimePeriod, limit: Int): Flow<List<TrackWithPlayCount>>

    fun getAlbumRank(album: Album, period: TimePeriod): Flow<Int>

    fun getAlbumRankByPlayCount(album: Album, period: TimePeriod): Flow<Int>

    fun getAlbumPlayCount(album: Album, period: TimePeriod): Flow<Int>

    fun getAlbumTimePlayed(album: Album, period: TimePeriod): Flow<Duration>

    fun getMostPlayedAlbums(period: TimePeriod): PagingSource<Int, AlbumWithTimePlayed>

    fun getMostPlayedAlbums(period: TimePeriod, limit: Int): Flow<List<AlbumWithTimePlayed>>

    fun getMostPlayedAlbumsByPlayCount(period: TimePeriod): PagingSource<Int, AlbumWithPlayCount>

    fun getMostPlayedAlbumsFromArtist(artist: Artist, period: TimePeriod): PagingSource<Int, AlbumWithTimePlayed>

    fun getMostPlayedAlbumsFromArtist(artist: Artist, period: TimePeriod, limit: Int): Flow<List<AlbumWithTimePlayed>>

    fun getMostPlayedAlbumsFromArtistByPlayCount(artist: Artist, period: TimePeriod): PagingSource<Int, AlbumWithPlayCount>

    fun getMostPlayedAlbumsFromArtistByPlayCount(artist: Artist, period: TimePeriod, limit: Int): Flow<List<AlbumWithPlayCount>>

    fun getTrackRank(track: Track, period: TimePeriod): Flow<Int>

    fun getTrackRankByPlayCount(track: Track, period: TimePeriod): Flow<Int>

    suspend fun insertEdit(edit: ScrobbleEdit): Long

    suspend fun updateEdit(edit: ScrobbleEdit)

    suspend fun deleteEdit(edit: ScrobbleEdit)

    fun getPlayCount(): Flow<Int>

    fun getEditCount(): Flow<Int>

    // oh my fucking god
    // TODO get rid of this when the paging library becomes good
    fun playEntityToObject(entity: ScrobbleWithData) =
        Scrobble(
            id = entity.id,
            metadata = entity.metadata.toTrack(),
            isDeleted = entity.isDeleted,
            timestamp = entity.timestamp,
            duration = entity.duration,
            timePlayed = entity.timePlayed,
            source = Source.user(entity.sourceDevice, entity.sourceApp),
            associatedEvents = entity.associatedEvents,
        )
}

class RoomRepository(
    private val db: AppDatabase,
) : Repository {
    private fun ArtistEntity.toObject() = Artist(name, artistId)

    private suspend fun AlbumEntity.toObject() =
        Album(
            name,
            ArtistList(getArtists(artistIds), artists),
            albumId,
        )

    private suspend fun TrackEntity.toObject() =
        Track(
            albumObject = albumId?.let { getAlbum(it) },
            name = name,
            artists = ArtistList(getArtists(artistIds), artists),
            id = trackId,
        )

    private suspend fun Scrobble.toEntity() =
        ScrobbleEntity(
            id = id,
            trackId = getOrInsertTrack(metadata),
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

    private suspend fun ScrobbleEntity.toObject() =
        Scrobble(
            id = id,
            metadata = getTrack(trackId).let { t ->
                t.copy(
                    artists = t.artists.copy(raw = artists),
                    albumObject = t.asAlbum?.copy(artists = t.albumArtists!!.copy(raw = albumArtists!!)),
                )
            },
            isDeleted = isDeleted,
            timestamp = timestamp,
            duration = duration,
            timePlayed = timePlayed,
            source = Source.user(sourceDevice, sourceApp),
            associatedEvents = associatedEvents,
        )

    private fun ScrobbleEdit.toEntity() =
        EditsEntity(
            id = id,
            scrobbleId = scrobbleId,
            trackId = track.id,
            timestamp = timestamp,
            reason = reason,
        )

    private suspend fun EditsEntity.toObject() =
        ScrobbleEdit(
            id = id,
            scrobbleId = scrobbleId,
            track = getTrack(trackId),
            timestamp = timestamp,
            reason = reason,
        )

    override suspend fun getArtist(id: Long) = db.trackDao().getArtist(id).toObject()

    override suspend fun getArtists(ids: List<Long>) = db.trackDao().getArtists(ids).map { it.toObject() }

    override suspend fun getAlbum(id: Long) = db.trackDao().getAlbum(id).toObject()

    override suspend fun getTrack(id: Long) = db.trackDao().getTrack(id).toObject()

    override suspend fun getOrInsertTrack(track: Track) =
        db.trackDao().getOrInsertTrack(
            track.name,
            track.artists,
            track.asAlbum?.name,
            track.asAlbum?.artists,
        )

    override suspend fun insertScrobble(scrobble: Scrobble) = db.scrobbleDao().insert(scrobble.toEntity())

    override suspend fun insertScrobbles(scrobbles: List<Scrobble>) = db.scrobbleDao().insertBatch(scrobbles.map { it.toEntity() })

    override suspend fun deleteScrobble(scrobble: Scrobble) = db.scrobbleDao().delete(scrobble.toEntity())

    override suspend fun updateScrobbles(scrobble: Scrobble) = db.scrobbleDao().update(scrobble.toEntity())

    override suspend fun updateScrobbleEvents(scrobbles: List<Scrobble>) =
        db.scrobbleDao().updateAssociatedEvents(scrobbles.map { it.toEntity() })

    override suspend fun getScrobble(id: Uuid) = db.scrobbleDao().getScrobble(id)?.toObject()

    override suspend fun getLastScrobbleFromSource(source: Source) =
        db.scrobbleDao().getLastScrobbleFromSource(source.client, source.app)?.toObject()

    override suspend fun getOriginalMetadata(scrobble: Scrobble) = db.editsDao().getOriginalMetadata(scrobble.id)?.let { getTrack(it) }

    override suspend fun markScrobbleDeleted(scrobble: Scrobble) = db.scrobbleDao().markScrobbleDeleted(scrobble.id)

    override fun getNowPlaying() = db.scrobbleDao().getNowPlaying().map { it.map { it.toObject() } }

    override fun getRecentScrobbles() = db.scrobbleDao().getRecentScrobbles()

    override fun getArtistPlayCount(artist: Artist, period: TimePeriod) =
        db.scrobbleDao().getArtistPlayCount(artist.id, period.start, period.end)

    override fun getArtistTimePlayed(artist: Artist, period: TimePeriod) =
        db.scrobbleDao().getArtistTimePlayed(artist.id, period.start, period.end)

    override fun getMostPlayedArtists(period: TimePeriod) = db.scrobbleDao().getMostPlayedArtists(period.start, period.end)

    override fun getMostPlayedArtists(period: TimePeriod, limit: Int) =
        db.scrobbleDao().getMostPlayedArtists(period.start, period.end, limit)

    override fun getMostPlayedArtistsByPlayCount(period: TimePeriod) =
        db.scrobbleDao().getMostPlayedArtistsByPlayCount(period.start, period.end)

    override fun getArtistRank(artist: Artist, period: TimePeriod) = db.scrobbleDao().getArtistRank(artist.id, period.start, period.end)

    override fun getArtistRankByPlayCount(artist: Artist, period: TimePeriod) =
        db.scrobbleDao().getArtistRankByPlayCount(artist.id, period.start, period.end)

    override fun getAlbumPlayCount(album: Album, period: TimePeriod) =
        db.scrobbleDao().getAlbumPlayCount(album.id, period.start, period.end)

    override fun getAlbumTimePlayed(album: Album, period: TimePeriod) =
        db.scrobbleDao().getAlbumTimePlayed(album.id, period.start, period.end)

    override fun getMostPlayedAlbums(period: TimePeriod) = db.scrobbleDao().getMostPlayedAlbums(period.start, period.end)

    override fun getMostPlayedAlbums(period: TimePeriod, limit: Int) = db.scrobbleDao().getMostPlayedAlbums(period.start, period.end, limit)

    override fun getMostPlayedAlbumsByPlayCount(period: TimePeriod) =
        db.scrobbleDao().getMostPlayedAlbumsByPlayCount(period.start, period.end)

    override fun getMostPlayedAlbumsFromArtist(artist: Artist, period: TimePeriod) =
        db.scrobbleDao().getMostPlayedAlbumsFromArtist(artist.id, period.start, period.end)

    override fun getMostPlayedAlbumsFromArtist(artist: Artist, period: TimePeriod, limit: Int) =
        db.scrobbleDao().getMostPlayedAlbumsFromArtist(artist.id, period.start, period.end, limit)

    override fun getMostPlayedAlbumsFromArtistByPlayCount(artist: Artist, period: TimePeriod) =
        db.scrobbleDao().getMostPlayedAlbumsFromArtistByPlayCount(artist.id, period.start, period.end)

    override fun getMostPlayedAlbumsFromArtistByPlayCount(artist: Artist, period: TimePeriod, limit: Int) =
        db.scrobbleDao().getMostPlayedAlbumsFromArtistByPlayCount(artist.id, period.start, period.end, limit)

    override fun getAlbumRank(album: Album, period: TimePeriod) = db.scrobbleDao().getAlbumRank(album.id, period.start, period.end)

    override fun getAlbumRankByPlayCount(album: Album, period: TimePeriod) =
        db.scrobbleDao().getAlbumRankByPlayCount(album.id, period.start, period.end)

    override fun getTrackPlayCount(track: Track, period: TimePeriod) =
        db.scrobbleDao().getTrackPlayCount(track.id, period.start, period.end)

    override fun getTrackTimePlayed(track: Track, period: TimePeriod) =
        db.scrobbleDao().getTrackTimePlayed(track.id, period.start, period.end)

    override fun getTrackHistory(track: Track) = db.scrobbleDao().getTrackHistory(track.id)

    override fun getMostPlayedTracks(period: TimePeriod) = db.scrobbleDao().getMostPlayedTracks(period.start, period.end)

    override fun getMostPlayedTracks(period: TimePeriod, limit: Int) = db.scrobbleDao().getMostPlayedTracks(period.start, period.end, limit)

    override fun getMostPlayedTracksByPlayCount(period: TimePeriod) =
        db.scrobbleDao().getMostPlayedTracksByPlayCount(period.start, period.end)

    override fun getMostPlayedTracksFromArtist(artist: Artist, period: TimePeriod) =
        db.scrobbleDao().getMostPlayedTracksFromArtist(artist.id, period.start, period.end)

    override fun getMostPlayedTracksFromArtistByPlayCount(artist: Artist, period: TimePeriod) =
        db.scrobbleDao().getMostPlayedTracksFromArtistByPlayCount(artist.id, period.start, period.end)

    override fun getMostPlayedTracksFromAlbum(album: Album, period: TimePeriod) =
        db.scrobbleDao().getMostPlayedTracksFromAlbum(album.id, period.start, period.end)

    override fun getMostPlayedTracksFromAlbumByPlayCount(album: Album, period: TimePeriod) =
        db.scrobbleDao().getMostPlayedTracksFromAlbumByPlayCount(album.id, period.start, period.end)

    override fun getMostPlayedTracksFromArtist(artist: Artist, period: TimePeriod, limit: Int) =
        db.scrobbleDao().getMostPlayedTracksFromArtist(artist.id, period.start, period.end, limit)

    override fun getMostPlayedTracksFromArtistByPlayCount(artist: Artist, period: TimePeriod, limit: Int) =
        db.scrobbleDao().getMostPlayedTracksFromArtistByPlayCount(artist.id, period.start, period.end, limit)

    override fun getMostPlayedTracksFromAlbum(album: Album, period: TimePeriod, limit: Int) =
        db.scrobbleDao().getMostPlayedTracksFromAlbum(album.id, period.start, period.end, limit)

    override fun getMostPlayedTracksFromAlbumByPlayCount(album: Album, period: TimePeriod, limit: Int) =
        db.scrobbleDao().getMostPlayedTracksFromAlbumByPlayCount(album.id, period.start, period.end, limit)

    override fun getTrackRank(track: Track, period: TimePeriod) = db.scrobbleDao().getTrackRank(track.id, period.start, period.end)

    override fun getTrackRankByPlayCount(track: Track, period: TimePeriod) =
        db.scrobbleDao().getTrackRankByPlayCount(track.id, period.start, period.end)

    override suspend fun insertEdit(edit: ScrobbleEdit) = db.editsDao().insert(edit.toEntity())

    override suspend fun updateEdit(edit: ScrobbleEdit) = db.editsDao().update(edit.toEntity())

    override suspend fun deleteEdit(edit: ScrobbleEdit) = db.editsDao().delete(edit.toEntity())

    override fun getPlayCount() = db.scrobbleDao().getPlayCount()

    override fun getEditCount() = db.editsDao().getEditCount()
}
