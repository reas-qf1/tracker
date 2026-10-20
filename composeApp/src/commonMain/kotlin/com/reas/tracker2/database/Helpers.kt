package com.reas.tracker2.database

import androidx.room3.Embedded
import androidx.room3.Junction
import androidx.room3.Relation
import com.reas.tracker2.database.entities.*
import com.reas.tracker2.shared.*
import kotlin.time.Duration

data class AlbumWithData(
    @Embedded val album: AlbumEntity,
    @Relation(
        parentColumns = ["albumId"],
        entityColumns = ["artistId"],
        associateBy = Junction(AlbumArtistCrossRef::class),
    )
    val artists: List<ArtistEntity>,
) {
    fun toObject() =
        Album(
            id = album.albumId,
            name = album.name,
            artists = ArtistList(artists.map { Artist(it.name, it.artistId) }, album.artists),
        )
}

data class TrackWithData(
    @Embedded val track: TrackEntity,
    @Relation(
        parentColumns = ["trackId"],
        entityColumns = ["artistId"],
        associateBy = Junction(TrackArtistCrossRef::class),
    )
    val artists: List<ArtistEntity>,
    @Relation(
        entity = AlbumEntity::class,
        parentColumns = ["albumId"],
        entityColumns = ["albumId"],
    )
    val album: AlbumWithData?,
) {
    fun toObject() =
        Track(
            id = track.trackId,
            name = track.name,
            artists = ArtistList(artists.map { Artist(it.name, it.artistId) }, track.artists),
            albumObject = album?.toObject(),
        )
}

data class ScrobbleWithData(
    @Embedded val scrobble: ScrobbleEntity,
    @Relation(
        entity = TrackEntity::class,
        parentColumns = ["trackId"],
        entityColumns = ["trackId"],
    )
    val metadata: TrackWithData,
) {
    fun toObject() =
        Scrobble(
            id = scrobble.id,
            metadata = metadata.toObject().let { t ->
                t.copy(
                    artists = t.artists.copy(raw = scrobble.artists),
                    albumObject = t.asAlbum?.copy(artists = t.albumArtists!!.copy(raw = scrobble.albumArtists!!)),
                )
            },
            isDeleted = scrobble.isDeleted,
            timestamp = scrobble.timestamp,
            duration = scrobble.duration,
            timePlayed = scrobble.timePlayed,
            source = Source.user(scrobble.sourceDevice, scrobble.sourceApp),
            associatedEvents = scrobble.associatedEvents,
        )
}

data class ArtistWithPlayCount(
    @Embedded val artist: Artist,
    val playCount: Int,
)

data class ArtistWithTimePlayed(
    @Embedded val artist: Artist,
    val timePlayed: Duration,
)

data class TrackWithPlayCount(
    @Embedded val _track: TrackWithData,
    val playCount: Int,
) {
    val track: Track
        get() = _track.toObject()
}

data class TrackWithTimePlayed(
    @Embedded val _track: TrackWithData,
    val timePlayed: Duration,
) {
    val track: Track
        get() = _track.toObject()
}

data class AlbumWithPlayCount(
    @Embedded val _album: AlbumWithData,
    val playCount: Int,
) {
    val album: Album
        get() = _album.toObject()
}

data class AlbumWithTimePlayed(
    @Embedded val _album: AlbumWithData,
    val timePlayed: Duration,
) {
    val album: Album
        get() = _album.toObject()
}
