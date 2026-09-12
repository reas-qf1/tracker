package com.reas.tracker2.database.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.reas.tracker2.shared.EventInfo
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "plays",
    indices = [
        Index("trackId", unique = false),
        Index("timestamp", orders = [Index.Order.DESC], unique = false),
        Index("sourceDevice", "sourceApp", "timestamp", unique = true)
    ]
)
data class PlayEntity(
    @PrimaryKey val id: Uuid,
    val trackId: Long,
    val artists: String,
    val albumArtists: String?,
    val timestamp: Instant,
    val duration: Duration,
    var timePlayed: Duration,
    var lastPosition: Duration,
    var lastPlaying: Boolean,
    val sourceDevice: String,
    val sourceApp: String,
    val associatedEvents: MutableList<EventInfo>
)