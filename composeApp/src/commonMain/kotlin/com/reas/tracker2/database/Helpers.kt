package com.reas.tracker2.database

import androidx.room3.Embedded
import androidx.room3.Junction
import androidx.room3.Relation
import com.reas.tracker2.database.entities.*
import com.reas.tracker2.shared.*
import kotlin.time.Duration
import kotlin.time.Instant

data class AlbumWithData(
    @Embedded val album: AlbumEntity,
    @Relation(
        parentColumns = ["albumId"],
        entityColumns = ["artistId"],
        associateBy = Junction(AlbumArtistCrossRef::class)
    )
    val artists: List<ArtistEntity>
) {
    fun toAlbum() = Album(
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
        associateBy = Junction(TrackArtistCrossRef::class)
    )
    val artists: List<ArtistEntity>,
    @Relation(
        entity = AlbumEntity::class,
        parentColumns = ["albumId"],
        entityColumns = ["albumId"]
    )
    val album: AlbumWithData?
) {
    fun toTrack() = TrackWithAlbum(
        trackObject = Track(
            id = track.trackId,
            name = track.name,
            artists = ArtistList(artists.map { Artist(it.name, it.artistId) }, track.artists)
        ),
        albumObject = album?.toAlbum()
    )
}

data class PlayWithData(
    val id: Long,
    @Embedded val metadata: TrackWithData,
    val timestamp: Instant,
    val duration: Duration,
    var timePlayed: Duration,
    var lastPosition: Duration,
    var lastPlaying: Boolean,
    val sourceDevice: String,
    val sourceApp: String,
    val associatedEvents: MutableList<EventInfo>
)

data class ArtistWithPlayCount(
    @Embedded val artist: Artist,
    val playCount: Int
)

data class ArtistWithTimePlayed(
    @Embedded val artist: Artist,
    val timePlayed: Duration
)

data class TrackWithPlayCount(
    @Embedded val _track: TrackWithData,
    val playCount: Int
) {
    val track: TrackWithAlbum
        get() = _track.toTrack()
}

data class TrackWithTimePlayed(
    @Embedded val _track: TrackWithData,
    val timePlayed: Duration
) {
    val track: TrackWithAlbum
        get() = _track.toTrack()
}

data class AlbumWithPlayCount(
    @Embedded val _album: AlbumWithData,
    val playCount: Int
) {
    val album: Album
        get() = _album.toAlbum()
}

data class AlbumWithTimePlayed(
    @Embedded val _album: AlbumWithData,
    val timePlayed: Duration
) {
    val album: Album
        get() = _album.toAlbum()
}