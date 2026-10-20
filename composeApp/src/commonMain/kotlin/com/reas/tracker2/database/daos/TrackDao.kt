package com.reas.tracker2.database.daos

import androidx.room3.*
import com.reas.tracker2.database.AlbumWithData
import com.reas.tracker2.database.TrackWithData
import com.reas.tracker2.database.entities.*

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(tracks: List<TrackEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbums(albums: List<AlbumEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtist(artist: ArtistEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtists(artists: List<ArtistEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbumArtistCrossRefs(x: List<AlbumArtistCrossRef>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrackArtistCrossRefs(x: List<TrackArtistCrossRef>)

    @Query("SELECT * FROM artists WHERE name IN (:names)")
    suspend fun getArtists(names: List<String>): List<ArtistEntity>

    @Transaction
    @Query("SELECT * FROM albums WHERE name = :name AND artistIds = :artistIds")
    suspend fun getAlbum(name: String, artistIds: List<Long>): AlbumWithData?

    @Transaction
    @Query("SELECT * FROM tracks WHERE name = :name AND artistIds = :artistIds AND albumId IS :albumId")
    suspend fun getTrack(name: String, artistIds: List<Long>, albumId: Long?): TrackWithData?

    @Transaction
    @Query("SELECT * FROM tracks WHERE trackId = :id")
    suspend fun getTrackById(id: Long): TrackWithData
}
