package com.reas.tracker2.database

import com.reas.tracker2.internalsServer.Repository
import com.reas.tracker2.shared.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import kotlin.uuid.Uuid

class DataRepository(
    private val db: Database,
) : Repository {
    override suspend fun <T> readTransaction(block: suspend () -> T): T =
        @Suppress("ktlint:standard:comment-wrapping")
        suspendTransaction(db/*, readOnly = true*/) { block() }

    override suspend fun <T> writeTransaction(block: suspend () -> T): T = suspendTransaction(db, readOnly = false) { block() }

    override suspend fun getArtists(names: List<String>): List<Artist> =
        readTransaction {
            ArtistTable
                .select(ArtistTable.id, ArtistTable.name)
                .where { ArtistTable.name inList names }
                .map { Artist(it[ArtistTable.name], it[ArtistTable.id].value) }
        }

    override suspend fun getAlbum(name: String, artists: ArtistList): Album? =
        readTransaction {
            val album = AlbumTable
                .selectAll()
                .where { AlbumTable.name eq name }
                .andWhere { AlbumTable.artistIds eq artists.ids.joinToString(",") }
                .singleOrNull()
            if (album == null) return@readTransaction null
            val artistIds = album[AlbumTable.artistIds].split(",").map { it.toLong() }
            val artists = getArtistsById(artistIds)
            return@readTransaction Album(
                id = album[AlbumTable.id].value,
                name = album[AlbumTable.name],
                artists = ArtistList(artists = artists, raw = album[AlbumTable.artists]),
            )
        }

    override suspend fun getTrack(name: String, artists: ArtistList, album: Album?): Track? =
        readTransaction {
            val track = TrackTable
                .selectAll()
                .where { TrackTable.name eq name }
                .andWhere { TrackTable.artistIds eq artists.ids.joinToString(",") }
                .singleOrNull()
            if (track == null) return@readTransaction null
            val artistIds = track[TrackTable.artistIds].split(",").map { it.toLong() }
            val artists = getArtistsById(artistIds)
            val album = track[TrackTable.albumId]?.let { getAlbumById(it.value) }
            return@readTransaction Track(
                id = track[TrackTable.id].value,
                name = track[TrackTable.name],
                artists = ArtistList(artists = artists, raw = track[TrackTable.artists]),
                albumObject = album,
            )
        }

    override suspend fun insertTrack(name: String, artists: ArtistList, album: Album?): Long =
        writeTransaction {
            TrackTable
                .insert {
                    it[TrackTable.name] = name
                    it[TrackTable.artists] = artists.raw
                    it[TrackTable.artistIds] = artists.ids.joinToString(",")
                    it[TrackTable.albumId] = album?.id
                }[TrackTable.id]
                .value
        }

    override suspend fun insertAlbum(name: String, artists: ArtistList): Long =
        writeTransaction {
            AlbumTable
                .insert {
                    it[AlbumTable.name] = name
                    it[AlbumTable.artists] = artists.raw
                    it[AlbumTable.artistIds] = artists.ids.joinToString(",")
                }[AlbumTable.id]
                .value
        }

    override suspend fun insertArtists(artists: List<String>): List<Long> =
        writeTransaction {
            ArtistTable
                .batchInsert(artists) {
                    this[ArtistTable.name] = it
                }.map { it[ArtistTable.id].value }
        }

    override suspend fun associateArtistsWithAlbum(albumId: Long, artists: ArtistList) {
        writeTransaction {
            AlbumArtistCrossRefTable.batchInsert(artists) {
                this[AlbumArtistCrossRefTable.albumId] = albumId
                this[AlbumArtistCrossRefTable.artistId] = it.id
            }
        }
    }

    override suspend fun associateArtistsWithTrack(trackId: Long, artists: ArtistList) {
        writeTransaction {
            TrackArtistCrossRefTable.batchInsert(artists) {
                this[TrackArtistCrossRefTable.trackId] = trackId
                this[TrackArtistCrossRefTable.artistId] = it.id
            }
        }
    }

    override suspend fun getScrobble(id: Uuid): Scrobble? =
        readTransaction {
            val scrobble = ScrobbleTable
                .selectAll()
                .where { ScrobbleTable.id eq id }
                .singleOrNull()
            scrobble?.let { resultRowToScrobble(it) }
        }

    override suspend fun insertScrobble(scrobble: Scrobble) {
        writeTransaction {
            val userId = UserTable
                .select(UserTable.id)
                .where { UserTable.name eq scrobble.source.user }
                .single()[UserTable.id]
            val clientId = ClientTable
                .select(ClientTable.id)
                .where { ClientTable.user eq userId }
                .andWhere { ClientTable.name eq scrobble.source.client }
                .single()[ClientTable.id]
            ScrobbleTable.upsert {
                it[ScrobbleTable.id] = scrobble.id
                it[ScrobbleTable.trackId] = scrobble.metadata.id
                it[ScrobbleTable.isDeleted] = scrobble.isDeleted
                it[ScrobbleTable.artists] = scrobble.artistsAsString
                it[ScrobbleTable.albumArtists] = scrobble.albumArtistsAsString
                it[ScrobbleTable.timestamp] = scrobble.timestamp
                it[ScrobbleTable.duration] = scrobble.duration
                it[ScrobbleTable.timePlayed] = scrobble.timePlayed
                it[ScrobbleTable.lastPosition] = scrobble.lastPosition
                it[ScrobbleTable.lastPlaying] = scrobble.lastPlaying
                it[ScrobbleTable.user] = userId
                it[ScrobbleTable.client] = clientId
                it[ScrobbleTable.app] = scrobble.app
                it[ScrobbleTable.associatedEvents] = scrobble.associatedEvents
            }
        }
    }

    override suspend fun insertScrobbles(scrobbles: List<Scrobble>) {
        writeTransaction {
            ScrobbleTable.batchUpsert(scrobbles) { scrobble ->
                val userId = UserTable
                    .select(UserTable.id)
                    .where { UserTable.name eq scrobble.source.user }
                    .single()[UserTable.id]
                val clientId = ClientTable
                    .select(ClientTable.id)
                    .where { ClientTable.user eq userId }
                    .andWhere { ClientTable.name eq scrobble.source.client }
                    .single()[ClientTable.id]
                this[ScrobbleTable.id] = scrobble.id
                this[ScrobbleTable.trackId] = scrobble.metadata.id
                this[ScrobbleTable.isDeleted] = scrobble.isDeleted
                this[ScrobbleTable.artists] = scrobble.artistsAsString
                this[ScrobbleTable.albumArtists] = scrobble.albumArtistsAsString
                this[ScrobbleTable.timestamp] = scrobble.timestamp
                this[ScrobbleTable.duration] = scrobble.duration
                this[ScrobbleTable.timePlayed] = scrobble.timePlayed
                this[ScrobbleTable.lastPosition] = scrobble.lastPosition
                this[ScrobbleTable.lastPlaying] = scrobble.lastPlaying
                this[ScrobbleTable.user] = userId
                this[ScrobbleTable.client] = clientId
                this[ScrobbleTable.app] = scrobble.app
                this[ScrobbleTable.associatedEvents] = scrobble.associatedEvents
            }
        }
    }

    override suspend fun updateScrobbleEvents(scrobbles: List<Scrobble>) {
        writeTransaction {
            for (scrobble in scrobbles) {
                ScrobbleTable.update({ ScrobbleTable.id eq scrobble.id }) {
                    it[ScrobbleTable.timePlayed] = scrobble.timePlayed
                    it[ScrobbleTable.lastPosition] = scrobble.lastPosition
                    it[ScrobbleTable.lastPlaying] = scrobble.lastPlaying
                    it[ScrobbleTable.associatedEvents] = scrobble.associatedEvents
                }
            }
        }
    }

    override suspend fun getNowPlaying(): List<Scrobble> =
        writeTransaction {
            ScrobbleTable
                .selectAll()
                .where { ScrobbleTable.lastPlaying eq true }
                .andWhere { ScrobbleTable.isDeleted eq false }
                .map { resultRowToScrobble(it) }
        }

    override suspend fun getLastScrobbleFromSource(source: Source): Scrobble? =
        readTransaction {
            val (userId, clientId) = getClientIdByName(source.user, source.client)
            ScrobbleTable
                .selectAll()
                .where { ScrobbleTable.user eq userId }
                .andWhere { ScrobbleTable.client eq clientId }
                .andWhere { ScrobbleTable.app eq source.app }
                .orderBy(ScrobbleTable.timestamp, SortOrder.DESC)
                .limit(1)
                .singleOrNull()
                ?.let { resultRowToScrobble(it) }
        }

    override suspend fun insertEdit(edit: ScrobbleEdit) {
        writeTransaction {
            EditTable.insert {
                it[EditTable.id] = edit.id
                it[EditTable.trackId] = edit.track.id
                it[EditTable.scrobbleId] = edit.scrobbleId
                it[EditTable.isDeleted] = edit.isDeleted
                it[EditTable.timestamp] = edit.timestamp
                it[EditTable.reason] = Json.encodeToString(edit.reason)
            }
        }
    }

    override suspend fun insertEdits(edits: List<ScrobbleEdit>) {
        writeTransaction {
            EditTable.batchInsert(edits) { edit ->
                this[EditTable.id] = edit.id
                this[EditTable.trackId] = edit.track.id
                this[EditTable.scrobbleId] = edit.scrobbleId
                this[EditTable.isDeleted] = edit.isDeleted
                this[EditTable.timestamp] = edit.timestamp
                this[EditTable.reason] = Json.encodeToString(edit.reason)
            }
        }
    }

    override suspend fun getOriginalMetadata(scrobble: Scrobble): Track? =
        readTransaction {
            EditTable
                .select(EditTable.trackId)
                .where { EditTable.scrobbleId eq scrobble.id }
                .singleOrNull()
                ?.let {
                    getTrackById(it[EditTable.trackId].value)
                }
        }

    private fun getUserIdByName(user: String) =
        UserTable
            .select(UserTable.id)
            .where { UserTable.name eq user }
            .single()[UserTable.id]
            .value

    private fun getClientIdByName(user: String, client: String): Pair<Uuid, Uuid> {
        val userId = getUserIdByName(user)
        val clientId = ClientTable
            .select(ClientTable.id)
            .where { ClientTable.name eq client }
            .andWhere { ClientTable.user eq userId }
            .single()[ClientTable.id]
            .value
        return userId to clientId
    }

    private fun getUserNameById(id: Uuid) =
        UserTable
            .select(UserTable.name)
            .where { UserTable.id eq id }
            .single()[UserTable.name]

    private fun getClientNameById(user: Uuid, client: Uuid): Pair<String, String> {
        val userId = getUserNameById(user)
        val clientId = ClientTable
            .select(ClientTable.name)
            .where { ClientTable.id eq client }
            .andWhere { ClientTable.user eq user }
            .single()[ClientTable.name]
        return userId to clientId
    }

    private fun getArtistsById(ids: List<Long>): List<Artist> =
        ArtistTable
            .select(ArtistTable.id, ArtistTable.name)
            .where { ArtistTable.id inList ids }
            .map { Artist(it[ArtistTable.name], it[ArtistTable.id].value) }

    private fun getAlbumById(id: Long): Album {
        val album = AlbumTable
            .selectAll()
            .where { AlbumTable.id eq id }
            .single()
        val artists = getArtistsById(album[AlbumTable.artistIds].split(",").map { it.toLong() })
        return Album(
            id = album[AlbumTable.id].value,
            name = album[AlbumTable.name],
            artists = ArtistList(raw = album[AlbumTable.artists], artists = artists),
        )
    }

    private fun getTrackById(id: Long): Track {
        val track = TrackTable
            .selectAll()
            .where { TrackTable.id eq id }
            .single()
        val artists = getArtistsById(track[TrackTable.artistIds].split(",").map { it.toLong() })
        val album = track[TrackTable.albumId]?.let { getAlbumById(it.value) }
        return Track(
            id = track[TrackTable.id].value,
            name = track[TrackTable.name],
            artists = ArtistList(raw = track[TrackTable.artists], artists = artists),
            albumObject = album,
        )
    }

    private fun resultRowToScrobble(row: ResultRow): Scrobble {
        val (user, client) = getClientNameById(row[ScrobbleTable.user].value, row[ScrobbleTable.client].value)
        return Scrobble(
            id = row[ScrobbleTable.id].value,
            metadata = getTrackById(row[ScrobbleTable.trackId].value).let { t ->
                t.copy(
                    artists = t.artists.copy(raw = row[ScrobbleTable.artists]),
                    albumObject = t.asAlbum?.copy(artists = t.albumArtists!!.copy(raw = row[ScrobbleTable.albumArtists]!!)),
                )
            },
            isDeleted = row[ScrobbleTable.isDeleted],
            duration = row[ScrobbleTable.duration],
            timePlayed = row[ScrobbleTable.timePlayed],
            timestamp = row[ScrobbleTable.timestamp],
            source = Source(user, client, row[ScrobbleTable.app]),
            associatedEvents = row[ScrobbleTable.associatedEvents].toMutableList(),
        )
    }
}
