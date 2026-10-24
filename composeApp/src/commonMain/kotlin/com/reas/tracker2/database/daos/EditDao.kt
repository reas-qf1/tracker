package com.reas.tracker2.database.daos

import androidx.room3.*
import androidx.room3.OnConflictStrategy.Companion.REPLACE
import com.reas.tracker2.database.EditWithMetadata
import com.reas.tracker2.database.entities.EditEntity
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface EditDao {
    @Insert(onConflict = REPLACE)
    suspend fun insert(play: EditEntity): Long

    @Update
    suspend fun update(play: EditEntity)

    @Delete
    suspend fun delete(play: EditEntity)

    @Query("SELECT * FROM edits WHERE scrobbleId = :scrobbleId ORDER BY timestamp ASC LIMIT 1")
    suspend fun getFirstEdit(scrobbleId: Uuid): EditWithMetadata?

    @Query("SELECT COUNT(*) FROM edits")
    fun getEditCount(): Flow<Int>
}
