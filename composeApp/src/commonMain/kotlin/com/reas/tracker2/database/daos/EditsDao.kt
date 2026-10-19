package com.reas.tracker2.database.daos

import androidx.room3.*
import androidx.room3.OnConflictStrategy.Companion.REPLACE
import com.reas.tracker2.database.entities.EditsEntity
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface EditsDao {
    @Insert(onConflict = REPLACE)
    suspend fun insert(play: EditsEntity): Long

    @Update
    suspend fun update(play: EditsEntity)

    @Delete
    suspend fun delete(play: EditsEntity)

    @Query("SELECT trackId FROM edits WHERE playId = :playId ORDER BY timestamp ASC LIMIT 1")
    suspend fun getOriginalMetadata(playId: Uuid): Long?

    @Query("SELECT COUNT(*) FROM edits")
    fun getEditCount(): Flow<Int>
}
