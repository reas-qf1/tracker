package com.reas.tracker2.api.v1.primitives

import com.reas.tracker2.api.helpers.TimestampSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class ScrobbleV1(
    val id: Uuid,
    val track: String,
    val artists: ArtistListV1,
    val album: String? = null,
    val albumArtists: ArtistListV1? = null,
    val isPlaying: Boolean,
    @Serializable(with = TimestampSerializer::class)
    val timestamp: Instant,
    val timePlayed: Duration,
    val duration: Duration,
    val device: String,
    val app: String,
    val other: JsonObject? = null,
)
