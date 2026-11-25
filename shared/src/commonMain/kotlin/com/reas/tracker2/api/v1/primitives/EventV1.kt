package com.reas.tracker2.api.v1.primitives

import com.reas.tracker2.api.helpers.DurationSerializer
import com.reas.tracker2.api.helpers.TimestampSerializer
import com.reas.tracker2.shared.RawArtistList
import com.reas.tracker2.shared.RawEvent
import com.reas.tracker2.shared.Source
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
data class EventV1(
    val track: String,
    val artist: String? = null,
    val artists: RawArtistListV1? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val albumArtists: RawArtistListV1? = null,
    val isPlaying: Boolean,
    @Serializable(with = TimestampSerializer::class)
    val timestamp: Instant,
    @Serializable(with = DurationSerializer::class)
    val position: Duration? = null,
    @Serializable(with = DurationSerializer::class)
    val duration: Duration? = null,
    val speed: Double = 1.0,
    val app: String? = null,
    val sessionId: String? = null,
    val other: JsonObject? = null,
) {
    init {
        require(track.isNotBlank()) { "track cannot be blank" }
        require(artist != null || artists != null) { "either artist or artists should be provided" }
        require(artist == null || artists == null) { "artist and artists cannot be provided at the same time" }
        require(artist == null || artist.isNotBlank()) { "artist cannot be blank" }
        require(artists == null || artists.list.isNotEmpty()) { "artists cannot be empty" }
        artists?.list?.forEachIndexed { i, a ->
            require(a.isNotBlank()) { "artist ${i + 1}: cannot be blank" }
        }
        require(album != null || albumArtist == null) { "albumArtist provided but no album" }
        require(album != null || albumArtists == null) { "albumArtists provided but no album" }
        if (album != null) {
            require(albumArtist == null || albumArtists == null) { "albumArtist and albumArtists cannot be provided at the same time" }
            require(albumArtist == null || albumArtist.isNotBlank()) { "albumArtist cannot be blank" }
            require(albumArtists == null || albumArtists.list.isNotEmpty()) { "albumArtists cannot be empty" }
            albumArtists?.list?.forEachIndexed { i, a ->
                require(a.isNotBlank()) { "album artist ${i + 1}: cannot be blank" }
            }
        }
        require((position == null && duration == null) || (position != null && duration != null)) {
            "position and duration must be either both specified or both omitted"
        }
    }

    fun toEvent(username: String, clientName: String) =
        RawEvent.create(
            track = track,
            artists = RawArtistList.create(
                raw = artist ?: artists!!.raw,
                list = artists?.list,
            ),
            album = album,
            albumArtists = if (albumArtist != null || albumArtists != null) {
                RawArtistList.create(
                    raw = albumArtist ?: albumArtists!!.raw,
                    list = albumArtists?.list,
                )
            } else {
                null
            },
            duration = duration ?: Duration.ZERO,
            timestamp = timestamp,
            position = position ?: Duration.ZERO,
            isPlaying = isPlaying,
            speed = speed,
            source = Source(username, clientName, app ?: "", sessionId ?: ""),
        )
}
