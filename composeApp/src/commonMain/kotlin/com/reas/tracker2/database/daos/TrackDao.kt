package com.reas.tracker2.database.daos

import androidx.room3.*
import com.reas.tracker2.database.entities.*
import com.reas.tracker2.shared.ArtistList

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(track: List<TrackEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbums(album: List<AlbumEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtist(artist: ArtistEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtists(artist: List<ArtistEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbumArtistCrossRefs(x: List<AlbumArtistCrossRef>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrackArtistCrossRefs(x: List<TrackArtistCrossRef>)

    @Query("SELECT * FROM artists WHERE name IN (:names)")
    suspend fun getArtistIds(names: List<String>): List<ArtistEntity>

    @Query("SELECT albumId FROM albums WHERE name = :name AND artistIds = :artistIds")
    suspend fun getAlbumId(name: String, artistIds: List<Long>): Long?

    @Query("SELECT trackId FROM tracks WHERE name = :name AND artistIds = :artistIds AND albumId IS :albumId")
    suspend fun getTrackId(name: String, artistIds: List<Long>, albumId: Long?): Long?

    @Query("SELECT * FROM artists WHERE artistId = :id")
    suspend fun getArtist(id: Long): ArtistEntity

    @Query("SELECT * FROM artists WHERE artistId in (:ids)")
    suspend fun getArtists(ids: List<Long>): List<ArtistEntity>

    @Query("SELECT * FROM albums WHERE albumId = :id")
    suspend fun getAlbum(id: Long): AlbumEntity

    @Query("SELECT * FROM tracks WHERE trackId = :id")
    suspend fun getTrack(id: Long): TrackEntity

    @Transaction
    suspend fun getOrInsertArtists(artists: List<String>): List<Long> {
        val existingArtists = getArtistIds(artists).associate { it.name to it.artistId }
        val newArtists = artists.filter { !existingArtists.containsKey(it) }
        if (newArtists.isEmpty()) {
            return artists.map { existingArtists[it]!! }
        }
        insertArtists(newArtists.map { ArtistEntity(name = it) })
        val newArtistIds = getArtistIds(newArtists).associate { it.name to it.artistId }
        return artists.map { existingArtists[it] ?: newArtistIds[it]!! }.sorted()
    }

    @Transaction
    suspend fun getOrInsertAlbum(name: String, artists: ArtistList): Long {
        val artistIds = getOrInsertArtists(artists.map { it.name })
        return getAlbumId(name, artistIds) ?: run {
            val albumId = insertAlbum(
                AlbumEntity(name = name, artistIds = artistIds, artists = artists.raw),
            )
            insertAlbumArtistCrossRefs(
                artistIds.map { artistId ->
                    AlbumArtistCrossRef(albumId, artistId)
                },
            )
            albumId
        }
    }

    @Transaction
    suspend fun getOrInsertTrack(
        name: String,
        artists: ArtistList,
        album: String?,
        albumArtists: ArtistList?,
    ): Long {
        val artistIds = getOrInsertArtists(artists.map { it.name })
        val albumId = album?.let { getOrInsertAlbum(album, albumArtists!!) }
        return getTrackId(name, artistIds, albumId) ?: run {
            val trackId = insertTrack(
                TrackEntity(name = name, albumId = albumId, artistIds = artistIds, artists = artists.raw),
            )
            insertTrackArtistCrossRefs(
                artistIds.map { artistId ->
                    TrackArtistCrossRef(trackId, artistId)
                },
            )
            trackId
        }
    }
}
