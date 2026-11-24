package com.reas.tracker2.api.v1.primitives

import com.reas.tracker2.api.helpers.DurationSerializer
import com.reas.tracker2.api.helpers.TimestampSerializer
import com.reas.tracker2.shared.Scrobble
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class ScrobbleV1(
    val id: Uuid,
    val track: IDObjectV1,
    val artists: ArtistListV1,
    val album: IDObjectV1? = null,
    val albumArtists: ArtistListV1? = null,
    val isPlaying: Boolean,
    @Serializable(with = TimestampSerializer::class)
    val timestamp: Instant,
    @Serializable(with = DurationSerializer::class)
    val timePlayed: Duration,
    @Serializable(with = DurationSerializer::class)
    val duration: Duration,
    val device: String,
    val app: String,
    val other: JsonObject? = null,
) {
    companion object {
        fun from(scrobble: Scrobble) =
            ScrobbleV1(
                id = scrobble.id,
                track = IDObjectV1(
                    id = scrobble.metadata.id,
                    name = scrobble.track,
                ),
                artists = ArtistListV1.from(scrobble.artists),
                album = scrobble.asAlbum?.let {
                    IDObjectV1(
                        id = it.id,
                        name = it.name,
                    )
                },
                albumArtists = scrobble.albumArtists?.let { ArtistListV1.from(it) },
                isPlaying = scrobble.lastPlaying,
                timestamp = scrobble.timestamp,
                timePlayed = scrobble.timePlayed,
                duration = scrobble.duration,
                device = scrobble.client,
                app = scrobble.app,
            )
    }
}
