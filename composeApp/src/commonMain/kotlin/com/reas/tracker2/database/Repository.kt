package com.reas.tracker2.database

import androidx.paging.PagingSource
import com.reas.tracker2.database.entities.AlbumEntity
import com.reas.tracker2.database.entities.ArtistEntity
import com.reas.tracker2.database.entities.PlayEntity
import com.reas.tracker2.database.entities.TrackEntity
import com.reas.tracker2.shared.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

interface Repository {
    suspend fun getArtist(id: Long): Artist
    suspend fun getArtists(ids: List<Long>): List<Artist>
    suspend fun getAlbum(id: Long): Album
    suspend fun getTrack(id: Long): TrackWithAlbum
    suspend fun getOrInsertArtists(artists: List<Artist>): List<Long>
    suspend fun getOrInsertAlbum(album: Album): Long
    suspend fun getOrInsertTrack(track: TrackWithAlbum): Long

    suspend fun insertPlay(play: Play): Long
    suspend fun insertPlays(plays: List<Play>)
    suspend fun updatePlay(play: Play)
    suspend fun deletePlay(play: Play)
    suspend fun getLastPlayFromSource(source: Source): Play?
    fun getNowPlayingTracks(): Flow<List<Play>>
    fun getRecentPlays(): PagingSource<Int, PlayWithData>

    fun getArtistPlays(artist: Artist, period: TimePeriod): Flow<Int>
    fun getArtistTimePlayed(artist: Artist, period: TimePeriod): Flow<Duration>
    fun getMostPlayedArtists(period: TimePeriod): PagingSource<Int, ArtistWithTimePlayed>
    fun getMostPlayedArtists(period: TimePeriod, limit: Int): Flow<List<ArtistWithTimePlayed>>
    fun getMostPlayedArtistsByPlayCount(period: TimePeriod): PagingSource<Int, ArtistWithPlayCount>
    fun getArtistRank(artist: Artist, period: TimePeriod): Flow<Int>
    fun getArtistRankByPlayCount(artist: Artist, period: TimePeriod) : Flow<Int>

    fun getTrackPlays(track: TrackWithAlbum, period: TimePeriod): Flow<Int>
    fun getTrackTimePlayed(track: TrackWithAlbum, period: TimePeriod): Flow<Duration>
    fun getTrackHistory(track: TrackWithAlbum): PagingSource<Int, PlayWithData>
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
    fun getAlbumRankByPlayCount(album: Album, period: TimePeriod) : Flow<Int>

    fun getAlbumPlays(album: Album, period: TimePeriod): Flow<Int>
    fun getAlbumTimePlayed(album: Album, period: TimePeriod): Flow<Duration>
    fun getMostPlayedAlbums(period: TimePeriod): PagingSource<Int, AlbumWithTimePlayed>
    fun getMostPlayedAlbums(period: TimePeriod, limit: Int): Flow<List<AlbumWithTimePlayed>>
    fun getMostPlayedAlbumsByPlayCount(period: TimePeriod): PagingSource<Int, AlbumWithPlayCount>
    fun getMostPlayedAlbumsFromArtist(artist: Artist, period: TimePeriod): PagingSource<Int, AlbumWithTimePlayed>
    fun getMostPlayedAlbumsFromArtist(artist: Artist, period: TimePeriod, limit: Int): Flow<List<AlbumWithTimePlayed>>
    fun getMostPlayedAlbumsFromArtistByPlayCount(artist: Artist, period: TimePeriod): PagingSource<Int, AlbumWithPlayCount>
    fun getMostPlayedAlbumsFromArtistByPlayCount(artist: Artist, period: TimePeriod, limit: Int): Flow<List<AlbumWithPlayCount>>
    fun getTrackRank(track: TrackWithAlbum, period: TimePeriod): Flow<Int>
    fun getTrackRankByPlayCount(track: TrackWithAlbum, period: TimePeriod) : Flow<Int>

    fun getPlayCount(): Flow<Int>

    // oh my fucking god
    // TODO get rid of this when the paging library becomes good
    fun playEntityToObject(entity: PlayWithData) = Play(
        id = entity.id,
        metadata = entity.metadata.toTrack(),
        timestamp = entity.timestamp,
        duration = entity.duration,
        timePlayed = entity.timePlayed,
        source = Source.user(entity.sourceDevice, entity.sourceApp),
        associatedEvents = entity.associatedEvents,
    )
}


class RoomRepository(private val db: AppDatabase) : Repository {
    private fun ArtistEntity.toObject() = Artist(name, artistId)
    private suspend fun AlbumEntity.toObject() = Album(
        name,
        ArtistList(getArtists(artistIds), artists),
        albumId
    )
    private suspend fun TrackEntity.toObject() = TrackWithAlbum(
        trackObject = Track(
            name,
            ArtistList(getArtists(artistIds), artists),
            trackId
        ),
        albumObject = albumId?.let { getAlbum(it) }
    )

    private suspend fun Play.toEntity() = PlayEntity(
        id = id,
        trackId = getOrInsertTrack(metadata),
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
    private suspend fun PlayEntity.toObject() = Play(
        id = id,
        metadata = getTrack(trackId).let { t ->
            TrackWithAlbum(
                trackObject = t.asTrack.copy(artists = t.artists.copy(raw = artists)),
                albumObject = t.asAlbum?.copy(artists = t.albumArtists!!.copy(raw = albumArtists!!))
            )
        },
        timestamp = timestamp,
        duration = duration,
        timePlayed = timePlayed,
        source = Source.user(sourceDevice, sourceApp),
        associatedEvents = associatedEvents,
    )

    override suspend fun getArtist(id: Long) = db.trackDao().getArtist(id).toObject()
    override suspend fun getArtists(ids: List<Long>) = db.trackDao().getArtists(ids).map { it.toObject() }
    override suspend fun getAlbum(id: Long) = db.trackDao().getAlbum(id).toObject()

    override suspend fun getTrack(id: Long) = db.trackDao().getTrack(id).toObject()

    override suspend fun getOrInsertArtists(artists: List<Artist>) = db.trackDao().getOrInsertArtists(artists.map { it.name })
    override suspend fun getOrInsertAlbum(album: Album) = db.trackDao().getOrInsertAlbum(album.name, album.artists)
    override suspend fun getOrInsertTrack(track: TrackWithAlbum) = db.trackDao().getOrInsertTrack(
        track.name,
        track.artists,
        track.asAlbum?.name,
        track.asAlbum?.artists
    )

    override suspend fun insertPlay(play: Play) = db.playDao().insert(play.toEntity())
    override suspend fun insertPlays(plays: List<Play>) = db.playDao().insertBatch(plays.map { it.toEntity() })
    override suspend fun deletePlay(play: Play) = db.playDao().delete(play.toEntity())
    override suspend fun updatePlay(play: Play) = db.playDao().update(play.toEntity())
    override suspend fun getLastPlayFromSource(source: Source) = db.playDao().getLastPlayFromSource(source.client, source.app)?.toObject()
    override fun getNowPlayingTracks() = db.playDao().getNowPlayingTracks().map { it.map { it.toObject() } }
    override fun getRecentPlays() = db.playDao().getRecentPlays()

    override fun getArtistPlays(artist: Artist, period: TimePeriod) = db.playDao().getArtistPlays(artist.id, period.start, period.end)
    override fun getArtistTimePlayed(artist: Artist, period: TimePeriod) = db.playDao().getArtistTimePlayed(artist.id, period.start, period.end)
    override fun getMostPlayedArtists(period: TimePeriod) = db.playDao().getMostPlayedArtists(period.start, period.end)
    override fun getMostPlayedArtists(period: TimePeriod, limit: Int) = db.playDao().getMostPlayedArtists(period.start, period.end, limit)
    override fun getMostPlayedArtistsByPlayCount(period: TimePeriod) = db.playDao().getMostPlayedArtistsByPlayCount(period.start, period.end)
    override fun getArtistRank(artist: Artist, period: TimePeriod) = db.playDao().getArtistRank(artist.id, period.start, period.end)
    override fun getArtistRankByPlayCount(artist: Artist, period: TimePeriod) = db.playDao().getArtistRankByPlayCount(artist.id, period.start, period.end)

    override fun getAlbumPlays(album: Album, period: TimePeriod) = db.playDao().getAlbumPlays(album.id, period.start, period.end)
    override fun getAlbumTimePlayed(album: Album, period: TimePeriod) = db.playDao().getAlbumTimePlayed(album.id, period.start, period.end)
    override fun getMostPlayedAlbums(period: TimePeriod) = db.playDao().getMostPlayedAlbums(period.start, period.end)
    override fun getMostPlayedAlbums(period: TimePeriod, limit: Int) = db.playDao().getMostPlayedAlbums(period.start, period.end, limit)
    override fun getMostPlayedAlbumsByPlayCount(period: TimePeriod) = db.playDao().getMostPlayedAlbumsByPlayCount(period.start, period.end)
    override fun getMostPlayedAlbumsFromArtist(artist: Artist, period: TimePeriod) = db.playDao().getMostPlayedAlbumsFromArtist(artist.id, period.start, period.end)
    override fun getMostPlayedAlbumsFromArtist(artist: Artist, period: TimePeriod, limit: Int) = db.playDao().getMostPlayedAlbumsFromArtist(artist.id, period.start, period.end, limit)
    override fun getMostPlayedAlbumsFromArtistByPlayCount(artist: Artist, period: TimePeriod) = db.playDao().getMostPlayedAlbumsFromArtistByPlayCount(artist.id, period.start, period.end)
    override fun getMostPlayedAlbumsFromArtistByPlayCount(artist: Artist, period: TimePeriod, limit: Int) = db.playDao().getMostPlayedAlbumsFromArtistByPlayCount(artist.id, period.start, period.end, limit)
    override fun getAlbumRank(album: Album, period: TimePeriod) = db.playDao().getAlbumRank(album.id, period.start, period.end)
    override fun getAlbumRankByPlayCount(album: Album, period: TimePeriod) = db.playDao().getAlbumRankByPlayCount(album.id, period.start, period.end)

    override fun getTrackPlays(track: TrackWithAlbum, period: TimePeriod) = db.playDao().getTrackPlays(track.id, period.start, period.end)
    override fun getTrackTimePlayed(track: TrackWithAlbum, period: TimePeriod) = db.playDao().getTrackTimePlayed(track.id, period.start, period.end)
    override fun getTrackHistory(track: TrackWithAlbum) = db.playDao().getTrackHistory(track.id)
    override fun getMostPlayedTracks(period: TimePeriod) = db.playDao().getMostPlayedTracks(period.start, period.end)
    override fun getMostPlayedTracks(period: TimePeriod, limit: Int) = db.playDao().getMostPlayedTracks(period.start, period.end, limit)
    override fun getMostPlayedTracksByPlayCount(period: TimePeriod) = db.playDao().getMostPlayedTracksByPlayCount(period.start, period.end)
    override fun getMostPlayedTracksFromArtist(artist: Artist, period: TimePeriod) = db.playDao().getMostPlayedTracksFromArtist(artist.id, period.start, period.end)
    override fun getMostPlayedTracksFromArtistByPlayCount(artist: Artist, period: TimePeriod) = db.playDao().getMostPlayedTracksFromArtistByPlayCount(artist.id, period.start, period.end)
    override fun getMostPlayedTracksFromAlbum(album: Album, period: TimePeriod) = db.playDao().getMostPlayedTracksFromAlbum(album.id, period.start, period.end)
    override fun getMostPlayedTracksFromAlbumByPlayCount(album: Album, period: TimePeriod) = db.playDao().getMostPlayedTracksFromAlbumByPlayCount(album.id, period.start, period.end)
    override fun getMostPlayedTracksFromArtist(artist: Artist, period: TimePeriod, limit: Int) = db.playDao().getMostPlayedTracksFromArtist(artist.id, period.start, period.end, limit)
    override fun getMostPlayedTracksFromArtistByPlayCount(artist: Artist, period: TimePeriod, limit: Int) = db.playDao().getMostPlayedTracksFromArtistByPlayCount(artist.id, period.start, period.end, limit)
    override fun getMostPlayedTracksFromAlbum(album: Album, period: TimePeriod, limit: Int) = db.playDao().getMostPlayedTracksFromAlbum(album.id, period.start, period.end, limit)
    override fun getMostPlayedTracksFromAlbumByPlayCount(album: Album, period: TimePeriod, limit: Int) = db.playDao().getMostPlayedTracksFromAlbumByPlayCount(album.id, period.start, period.end, limit)
    override fun getTrackRank(track: TrackWithAlbum, period: TimePeriod) = db.playDao().getTrackRank(track.id, period.start, period.end)
    override fun getTrackRankByPlayCount(track: TrackWithAlbum, period: TimePeriod) = db.playDao().getTrackRankByPlayCount(track.id, period.start, period.end)

    override fun getPlayCount() = db.playDao().getPlayCount()
}