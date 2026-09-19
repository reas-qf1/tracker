package com.reas.tracker2.database.daos

import androidx.paging.PagingSource
import androidx.room3.*
import androidx.room3.OnConflictStrategy.Companion.REPLACE
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import com.reas.tracker2.database.*
import com.reas.tracker2.database.entities.PlayEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Instant

private const val inTimeRange = "timestamp >= :start AND timestamp < :end"
private const val isFullPlay = "timePlayed >= MIN(duration / 2, 240 * 1000)"
private const val isNotSkip = "timePlayed >= 2 * 1000"

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface PlayDao {
    @Insert(onConflict = REPLACE)
    suspend fun insert(play: PlayEntity): Long

    @Insert(onConflict = REPLACE)
    suspend fun insertBatch(plays: List<PlayEntity>)

    @Update
    suspend fun update(play: PlayEntity)

    @Delete
    suspend fun delete(play: PlayEntity)

    @Query("SELECT * FROM plays WHERE sourceDevice = :device AND sourceApp = :app ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPlayFromSource(device: String?, app: String?): PlayEntity?

    @Query("SELECT * FROM plays WHERE lastPlaying = 1")
    fun getNowPlayingTracks(): Flow<List<PlayEntity>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
        WITH t0 as (
            SELECT * FROM
                (SELECT *, 1 as sort1, 0 as sort2 FROM plays WHERE lastPlaying = 1)
            UNION ALL SELECT * FROM
                (SELECT *, 2 as sort1, timestamp as sort2 FROM plays WHERE $isFullPlay AND lastPlaying = 0)
        ) SELECT t0.*, tracks.* FROM t0
        JOIN tracks ON t0.trackId = tracks.trackId
        ORDER BY sort1, sort2 DESC
    """,
    )
    fun getRecentPlays(): PagingSource<Int, PlayWithData>

    @Query(
        """
        SELECT COUNT(*) FROM plays
        JOIN track_artists ON plays.trackId = track_artists.trackId
        WHERE track_artists.artistId = :artistId AND $inTimeRange AND $isFullPlay
    """,
    )
    fun getArtistPlays(artistId: Long, start: Instant, end: Instant): Flow<Int>

    @Query(
        """
        SELECT SUM(timePlayed) FROM plays
        JOIN track_artists ON plays.trackId = track_artists.trackId
        WHERE track_artists.artistId = :artistId AND $inTimeRange AND $isNotSkip
    """,
    )
    fun getArtistTimePlayed(artistId: Long, start: Instant, end: Instant): Flow<Duration>

    @Query(
        """
        SELECT artists.artistId as id, artists.name as name, SUM(timePlayed) as timePlayed FROM plays
        JOIN track_artists ON plays.trackId = track_artists.trackId
        JOIN artists ON track_artists.artistId = artists.artistId
        WHERE $inTimeRange AND $isNotSkip
        GROUP BY name ORDER BY timePlayed DESC
    """,
    )
    fun getMostPlayedArtists(start: Instant, end: Instant): PagingSource<Int, ArtistWithTimePlayed>

    @Query(
        """
        SELECT artists.artistId as id, artists.name as name, SUM(timePlayed) as timePlayed FROM plays
        JOIN track_artists ON plays.trackId = track_artists.trackId
        JOIN artists ON track_artists.artistId = artists.artistId
        WHERE $inTimeRange AND $isNotSkip
        GROUP BY name ORDER BY timePlayed DESC LIMIT :limit
    """,
    )
    fun getMostPlayedArtists(start: Instant, end: Instant, limit: Int): Flow<List<ArtistWithTimePlayed>>

    @Query(
        """
        SELECT artists.artistId as id, artists.name as name, COUNT(*) as playCount FROM plays
        JOIN track_artists ON plays.trackId = track_artists.trackId
        JOIN artists ON track_artists.artistId = artists.artistId
        WHERE $inTimeRange AND $isFullPlay
        GROUP BY name ORDER BY playCount DESC
    """,
    )
    fun getMostPlayedArtistsByPlayCount(start: Instant, end: Instant): PagingSource<Int, ArtistWithPlayCount>

    @Query(
        """
        WITH t0 AS (
            SELECT track_artists.artistId as artistId, SUM(timePlayed) as metric FROM plays
            JOIN track_artists ON plays.trackId = track_artists.trackId
            WHERE $inTimeRange AND $isNotSkip GROUP BY artistId
        ) SELECT COUNT(*) FROM t0 WHERE metric > (SELECT metric FROM t0 WHERE artistId = :artistId)
    """,
    )
    fun getArtistRank(artistId: Long, start: Instant, end: Instant): Flow<Int>

    @Query(
        """
        WITH t0 AS (
            SELECT track_artists.artistId as artistId, COUNT(*) as metric FROM plays
            JOIN track_artists ON plays.trackId = track_artists.trackId
            WHERE $inTimeRange AND $isFullPlay GROUP BY artistId
        ) SELECT COUNT(*) FROM t0 WHERE metric > (SELECT metric FROM t0 WHERE artistId = :artistId)
    """,
    )
    fun getArtistRankByPlayCount(artistId: Long, start: Instant, end: Instant): Flow<Int>

    @Query("SELECT COUNT(*) FROM plays WHERE trackId = :trackId AND $inTimeRange AND $isFullPlay")
    fun getTrackPlays(trackId: Long, start: Instant, end: Instant): Flow<Int>

    @Query("SELECT SUM(timePlayed) FROM plays WHERE trackId = :trackId AND $inTimeRange AND $isNotSkip")
    fun getTrackTimePlayed(trackId: Long, start: Instant, end: Instant): Flow<Duration>

    @Transaction
    @Query(
        """
        SELECT plays.*, tracks.* FROM plays
        JOIN tracks ON plays.trackId = tracks.trackId
        WHERE plays.trackId = :trackId AND $isFullPlay ORDER BY timestamp DESC
    """,
    )
    fun getTrackHistory(trackId: Long): PagingSource<Int, PlayWithData>

    @Transaction
    @Query(
        """
        SELECT tracks.*, SUM(timePlayed) as timePlayed FROM plays
        JOIN tracks ON tracks.trackId = plays.trackId
        WHERE $inTimeRange AND $isNotSkip
        GROUP BY tracks.trackId ORDER BY timePlayed DESC
    """,
    )
    fun getMostPlayedTracks(start: Instant, end: Instant): PagingSource<Int, TrackWithTimePlayed>

    @Transaction
    @Query(
        """
        SELECT tracks.*, SUM(timePlayed) as timePlayed FROM plays
        JOIN tracks ON tracks.trackId = plays.trackId
        WHERE $inTimeRange AND $isNotSkip
        GROUP BY tracks.trackId ORDER BY timePlayed DESC LIMIT :limit
    """,
    )
    fun getMostPlayedTracks(start: Instant, end: Instant, limit: Int): Flow<List<TrackWithTimePlayed>>

    @Transaction
    @Query(
        """
        SELECT tracks.*, COUNT(*) as playCount FROM plays
        JOIN tracks ON tracks.trackId = plays.trackId
        WHERE $inTimeRange AND $isFullPlay
        GROUP BY tracks.trackId ORDER BY playCount DESC
    """,
    )
    fun getMostPlayedTracksByPlayCount(start: Instant, end: Instant): PagingSource<Int, TrackWithPlayCount>

    @Transaction
    @Query(
        """
        SELECT tracks.*, SUM(timePlayed) as timePlayed FROM plays
        JOIN tracks ON tracks.trackId = plays.trackId
        JOIN track_artists ON plays.trackId = track_artists.trackId
        WHERE $inTimeRange AND $isNotSkip AND track_artists.artistId = :artistId
        GROUP BY plays.trackId ORDER BY timePlayed DESC
    """,
    )
    fun getMostPlayedTracksFromArtist(artistId: Long, start: Instant, end: Instant): PagingSource<Int, TrackWithTimePlayed>

    @Transaction
    @Query(
        """
        SELECT tracks.*, SUM(timePlayed) as timePlayed FROM plays
        JOIN tracks ON tracks.trackId = plays.trackId
        JOIN track_artists ON plays.trackId = track_artists.trackId
        WHERE $inTimeRange AND $isNotSkip AND track_artists.artistId = :artistId
        GROUP BY plays.trackId ORDER BY timePlayed DESC LIMIT :limit
    """,
    )
    fun getMostPlayedTracksFromArtist(
        artistId: Long,
        start: Instant,
        end: Instant,
        limit: Int,
    ): Flow<List<TrackWithTimePlayed>>

    @Transaction
    @Query(
        """
        SELECT tracks.*, COUNT(*) as playCount FROM plays
        JOIN tracks ON tracks.trackId = plays.trackId
        JOIN track_artists ON plays.trackId = track_artists.trackId
        WHERE $inTimeRange AND $isFullPlay AND track_artists.artistId = :artistId
        GROUP BY plays.trackId ORDER BY playCount DESC
    """,
    )
    fun getMostPlayedTracksFromArtistByPlayCount(artistId: Long, start: Instant, end: Instant): PagingSource<Int, TrackWithPlayCount>

    @Transaction
    @Query(
        """
        SELECT tracks.*, COUNT(*) as playCount FROM plays
        JOIN tracks ON tracks.trackId = plays.trackId
        JOIN track_artists ON plays.trackId = track_artists.trackId
        WHERE $inTimeRange AND $isFullPlay AND track_artists.artistId = :artistId
        GROUP BY plays.trackId ORDER BY playCount DESC LIMIT :limit
    """,
    )
    fun getMostPlayedTracksFromArtistByPlayCount(
        artistId: Long,
        start: Instant,
        end: Instant,
        limit: Int,
    ): Flow<List<TrackWithPlayCount>>

    @Transaction
    @Query(
        """
        SELECT tracks.*, SUM(timePlayed) as timePlayed FROM plays
        JOIN tracks ON plays.trackId = tracks.trackId
        WHERE $inTimeRange AND $isNotSkip AND tracks.albumId = :albumId
        GROUP BY plays.trackId ORDER BY timePlayed DESC
    """,
    )
    fun getMostPlayedTracksFromAlbum(albumId: Long, start: Instant, end: Instant): PagingSource<Int, TrackWithTimePlayed>

    @Transaction
    @Query(
        """
        SELECT tracks.*, SUM(timePlayed) as timePlayed FROM plays
        JOIN tracks ON plays.trackId = tracks.trackId
        WHERE $inTimeRange AND $isNotSkip AND tracks.albumId = :albumId
        GROUP BY plays.trackId ORDER BY timePlayed DESC LIMIT :limit
    """,
    )
    fun getMostPlayedTracksFromAlbum(
        albumId: Long,
        start: Instant,
        end: Instant,
        limit: Int,
    ): Flow<List<TrackWithTimePlayed>>

    @Transaction
    @Query(
        """
        SELECT tracks.*, COUNT(*) as playCount FROM plays
        JOIN tracks ON plays.trackId = tracks.trackId
        WHERE $inTimeRange AND $isFullPlay AND tracks.albumId = :albumId
        GROUP BY plays.trackId ORDER BY playCount DESC
    """,
    )
    fun getMostPlayedTracksFromAlbumByPlayCount(albumId: Long, start: Instant, end: Instant): PagingSource<Int, TrackWithPlayCount>

    @Transaction
    @Query(
        """
        SELECT tracks.*, COUNT(*) as playCount FROM plays
        JOIN tracks ON plays.trackId = tracks.trackId
        WHERE $inTimeRange AND $isFullPlay AND tracks.albumId = :albumId
        GROUP BY plays.trackId ORDER BY playCount DESC LIMIT :limit
    """,
    )
    fun getMostPlayedTracksFromAlbumByPlayCount(
        albumId: Long,
        start: Instant,
        end: Instant,
        limit: Int,
    ): Flow<List<TrackWithPlayCount>>

    @Query(
        """
        WITH t0 AS (
            SELECT trackId, SUM(timePlayed) as metric FROM plays
            WHERE $inTimeRange AND $isNotSkip GROUP BY trackId
        ) SELECT COUNT(*) FROM t0 WHERE metric > (SELECT metric FROM t0 WHERE trackId = :trackId)
    """,
    )
    fun getTrackRank(trackId: Long, start: Instant, end: Instant): Flow<Int>

    @Query(
        """
        WITH t0 AS (
            SELECT trackId, COUNT(*) as metric FROM plays
            WHERE $inTimeRange AND $isFullPlay GROUP BY trackId
        ) SELECT COUNT(*) FROM t0 WHERE metric > (SELECT metric FROM t0 WHERE trackId = :trackId)
    """,
    )
    fun getTrackRankByPlayCount(trackId: Long, start: Instant, end: Instant): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM plays
        JOIN tracks ON plays.trackId = tracks.trackId
        WHERE tracks.albumId = :albumId AND $inTimeRange AND $isFullPlay
    """,
    )
    fun getAlbumPlays(albumId: Long, start: Instant, end: Instant): Flow<Int>

    @Query(
        """
        SELECT SUM(timePlayed) FROM plays
        JOIN tracks ON plays.trackId = tracks.trackId
        WHERE tracks.albumId = :albumId AND $inTimeRange AND $isNotSkip
    """,
    )
    fun getAlbumTimePlayed(albumId: Long, start: Instant, end: Instant): Flow<Duration>

    @Transaction
    @Query(
        """
        SELECT albums.*, SUM(timePlayed) as timePlayed FROM plays
        JOIN tracks ON plays.trackId = tracks.trackId
        JOIN albums ON tracks.albumId = albums.albumId
        WHERE $inTimeRange AND $isNotSkip
        GROUP BY albums.albumId ORDER BY timePlayed DESC
    """,
    )
    fun getMostPlayedAlbums(start: Instant, end: Instant): PagingSource<Int, AlbumWithTimePlayed>

    @Transaction
    @Query(
        """
        SELECT albums.*, SUM(timePlayed) as timePlayed FROM plays
        JOIN tracks ON plays.trackId = tracks.trackId
        JOIN albums ON tracks.albumId = albums.albumId
        WHERE $inTimeRange AND $isNotSkip
        GROUP BY albums.albumId ORDER BY timePlayed DESC LIMIT :limit
    """,
    )
    fun getMostPlayedAlbums(start: Instant, end: Instant, limit: Int): Flow<List<AlbumWithTimePlayed>>

    @Transaction
    @Query(
        """
        SELECT albums.*, COUNT(*) as playCount FROM plays
        JOIN tracks ON plays.trackId = tracks.trackId
        JOIN albums ON tracks.albumId = albums.albumId
        WHERE $inTimeRange AND $isFullPlay
        GROUP BY albums.albumId ORDER BY playCount DESC
    """,
    )
    fun getMostPlayedAlbumsByPlayCount(start: Instant, end: Instant): PagingSource<Int, AlbumWithPlayCount>

    @Transaction
    @Query(
        """
        SELECT albums.*, SUM(timePlayed) as timePlayed FROM plays
        JOIN tracks ON plays.trackId = tracks.trackId
        JOIN albums ON tracks.albumId = albums.albumId
        JOIN album_artists ON albums.albumId = album_artists.albumId
        WHERE $inTimeRange AND $isNotSkip AND album_artists.artistId = :artistId
        GROUP BY albums.albumId ORDER BY timePlayed DESC
    """,
    )
    fun getMostPlayedAlbumsFromArtist(artistId: Long, start: Instant, end: Instant): PagingSource<Int, AlbumWithTimePlayed>

    @Transaction
    @Query(
        """
        SELECT albums.*, SUM(timePlayed) as timePlayed FROM plays
        JOIN tracks ON plays.trackId = tracks.trackId
        JOIN albums ON tracks.albumId = albums.albumId
        JOIN album_artists ON albums.albumId = album_artists.albumId
        WHERE $inTimeRange AND $isNotSkip AND album_artists.artistId = :artistId
        GROUP BY albums.albumId ORDER BY timePlayed DESC LIMIT :limit
    """,
    )
    fun getMostPlayedAlbumsFromArtist(
        artistId: Long,
        start: Instant,
        end: Instant,
        limit: Int,
    ): Flow<List<AlbumWithTimePlayed>>

    @Transaction
    @Query(
        """
        SELECT albums.*, COUNT(*) as playCount FROM plays
        JOIN tracks ON plays.trackId = tracks.trackId
        JOIN albums ON tracks.albumId = albums.albumId
        JOIN album_artists ON albums.albumId = album_artists.albumId
        WHERE $inTimeRange AND $isFullPlay AND album_artists.artistId = :artistId
        GROUP BY albums.albumId ORDER BY playCount DESC
    """,
    )
    fun getMostPlayedAlbumsFromArtistByPlayCount(artistId: Long, start: Instant, end: Instant): PagingSource<Int, AlbumWithPlayCount>

    @Transaction
    @Query(
        """
        SELECT albums.*, COUNT(*) as playCount FROM plays
        JOIN tracks ON plays.trackId = tracks.trackId
        JOIN albums ON tracks.albumId = albums.albumId
        JOIN album_artists ON albums.albumId = album_artists.albumId
        WHERE $inTimeRange AND $isFullPlay AND album_artists.artistId = :artistId
        GROUP BY albums.albumId ORDER BY playCount DESC LIMIT :limit
    """,
    )
    fun getMostPlayedAlbumsFromArtistByPlayCount(
        artistId: Long,
        start: Instant,
        end: Instant,
        limit: Int,
    ): Flow<List<AlbumWithPlayCount>>

    @Query(
        """
        WITH t0 AS (
            SELECT tracks.albumId as id, SUM(timePlayed) as metric FROM plays
            JOIN tracks ON plays.trackId = tracks.trackId
            WHERE $inTimeRange AND $isNotSkip GROUP BY id
        ) SELECT COUNT(*) FROM t0 WHERE metric > (SELECT metric FROM t0 WHERE id = :albumId)
    """,
    )
    fun getAlbumRank(albumId: Long, start: Instant, end: Instant): Flow<Int>

    @Query(
        """
        WITH t0 AS (
            SELECT tracks.albumId as id, COUNT(*) as metric FROM plays
            JOIN tracks ON plays.trackId = tracks.trackId
            WHERE $inTimeRange AND $isFullPlay GROUP BY id
        ) SELECT COUNT(*) FROM t0 WHERE metric > (SELECT metric FROM t0 WHERE id = :albumId)
    """,
    )
    fun getAlbumRankByPlayCount(albumId: Long, start: Instant, end: Instant): Flow<Int>

    @Query("SELECT COUNT(*) FROM plays")
    fun getPlayCount(): Flow<Int>
}
