package com.reas.tracker2.database.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [Index("name", "albumId", "artistIds", unique = true)],
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val trackId: Long = 0,
    val name: String,
    val albumId: Long? = null,
    val artists: String,
    val artistIds: List<Long>,
)

@Entity(
    tableName = "albums",
    indices = [Index("name", "artistIds", unique = true)],
)
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val albumId: Long = 0,
    val name: String,
    val artists: String,
    val artistIds: List<Long>,
)

@Entity(
    tableName = "artists",
    indices = [Index("name", unique = true)],
)
data class ArtistEntity(
    @PrimaryKey(autoGenerate = true)
    val artistId: Long = 0,
    val name: String,
)

@Entity(
    tableName = "track_artists",
    primaryKeys = ["trackId", "artistId"],
    indices = [Index(value = ["artistId"])],
)
data class TrackArtistCrossRef(
    val trackId: Long,
    val artistId: Long,
)

@Entity(
    tableName = "album_artists",
    primaryKeys = ["albumId", "artistId"],
    indices = [Index(value = ["artistId"])],
)
data class AlbumArtistCrossRef(
    val albumId: Long,
    val artistId: Long,
)
