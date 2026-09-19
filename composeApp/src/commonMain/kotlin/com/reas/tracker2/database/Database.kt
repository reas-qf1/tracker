package com.reas.tracker2.database

import androidx.room3.*
import com.reas.tracker2.database.daos.PlayDao
import com.reas.tracker2.database.daos.TrackDao
import com.reas.tracker2.database.entities.*
import kotlinx.coroutines.Dispatchers

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

@Database(
    entities = [
        PlayEntity::class,
        TrackEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        TrackArtistCrossRef::class,
        AlbumArtistCrossRef::class,
    ],
    version = 1,
    exportSchema = false,
)
@ColumnTypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playDao(): PlayDao

    abstract fun trackDao(): TrackDao

    companion object {
        fun getDatabase(builder: Builder<AppDatabase>): AppDatabase =
            builder
                .setQueryCoroutineContext(Dispatchers.IO)
                .fallbackToDestructiveMigration(false)
                .build()
    }
}
