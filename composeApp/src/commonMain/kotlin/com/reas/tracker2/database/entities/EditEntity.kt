package com.reas.tracker2.database.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.reas.tracker2.shared.ScrobbleEdit
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "edits",
    indices = [Index(value = ["scrobbleId", "timestamp"], unique = false)],
)
data class EditEntity(
    @PrimaryKey val id: Uuid,
    val scrobbleId: Uuid,
    val trackId: Long,
    val isDeleted: Boolean,
    val timestamp: Instant,
    val reason: ScrobbleEdit.EditReason,
)
