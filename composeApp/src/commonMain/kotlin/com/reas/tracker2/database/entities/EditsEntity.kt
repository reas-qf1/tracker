package com.reas.tracker2.database.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.reas.tracker2.shared.PlayEdit
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "edits",
    indices = [Index(value = ["playId", "timestamp"], unique = false)],
)
data class EditsEntity(
    @PrimaryKey val id: Uuid,
    val playId: Uuid,
    val trackId: Long,
    val timestamp: Instant,
    val reason: PlayEdit.EditReason,
)
