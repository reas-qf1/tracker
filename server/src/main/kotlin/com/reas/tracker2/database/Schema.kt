package com.reas.tracker2.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

object UserTable : UUIDTable("users") {
    val name = text("name").uniqueIndex()
    val passwordHash = text("password_hash")
}

object ClientTable : UUIDTable("clients") {
    val user = reference("user", UserTable.id)
    val name = text("name").index()

    val index = uniqueIndex(user, name)
}

object TokenTable : UUIDTable("tokens") {
    val user = reference("user", UserTable.id)
    val client = reference("client", ClientTable.id)
    val tokenPrefix = varchar("token_prefix", 8)
    val tokenHash = binary("token_hash", 32)
    val createdAt = long("created_at")
    val expiresAt = long("expires_at")
}

object ArtistTable : LongIdTable("artists") {
    val name = text("name").uniqueIndex()
}

object AlbumTable : LongIdTable("albums") {
    val name = text("name")
    val artists = text("artists")
    val artistIds = text("artist_ids")

    val index = uniqueIndex(name, artistIds)
}

object TrackTable : LongIdTable("tracks") {
    val name = text("name")
    val albumId = reference("album_id", AlbumTable.id).nullable()
    val artists = text("artists")
    val artistIds = text("artist_ids")

    val index = uniqueIndex(name, albumId, artistIds)
}

object AlbumArtistCrossRefTable : Table("album_artists") {
    val albumId = reference("album_id", AlbumTable.id)
    val artistId = reference("artist_id", ArtistTable.id)

    val index1 = uniqueIndex(albumId, artistId)
    val index2 = uniqueIndex(artistId, albumId)
}

object TrackArtistCrossRefTable : Table("track_artists") {
    val trackId = reference("track_id", TrackTable.id)
    val artistId = reference("artist_id", ArtistTable.id)

    val index1 = uniqueIndex(trackId, artistId)
    val index2 = uniqueIndex(artistId, trackId)
}

object ScrobbleTable : UUIDTable("scrobbles") {
    val user = reference("user", UserTable.id)
    val client = reference("client", ClientTable.id)
    val trackId = reference("track_id", TrackTable.id).index()
    val isDeleted = bool("is_deleted")
    val artists = text("artists")
    val albumArtists = text("album_artists").nullable()
    val timestamp = long("timestamp").index()
    val duration = long("duration")
    val timePlayed = long("time_played")
    val lastPosition = long("last_position")
    val lastPlaying = bool("last_playing")
    val app = text("app")
    val associatedEvents = text("associated_events")
    val other = text("other").nullable()

    val sourceIndex = index(isUnique = false, user, client, app)
}

object EditTable : UUIDTable("edits") {
    val scrobbleId = reference("scrobble_id", ScrobbleTable.id)
    val trackId = reference("track_id", TrackTable.id)
    val isDeleted = bool("is_deleted")
    val timestamp = long("timestamp")
    val reason = text("reason")

    val index = index(isUnique = false, scrobbleId, trackId)
}
