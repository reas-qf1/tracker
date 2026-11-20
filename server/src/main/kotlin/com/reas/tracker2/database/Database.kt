package com.reas.tracker2.database

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun createTables(db: Database) =
    transaction(db) {
        SchemaUtils.create(
            UserTable,
            ClientTable,
            TokenTable,
            ArtistTable,
            AlbumTable,
            TrackTable,
            AlbumArtistCrossRefTable,
            TrackArtistCrossRefTable,
            EditTable,
        )
    }

fun createInMemoryDatabase(): Database {
    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = "",
        databaseConfig = DatabaseConfig {
            useNestedTransactions = true
            defaultMinRetryDelay = 10
            defaultMaxRetryDelay = 1000
        },
    )
    createTables(database)
    return database
}

fun createSQLiteDatabase(): Database {
    val database = Database.connect(
        url = "jdbc:sqlite:server.db",
        driver = "org.sqlite.JDBC",
        databaseConfig = DatabaseConfig {
            useNestedTransactions = true
            defaultMinRetryDelay = 10
            defaultMaxRetryDelay = 1000
        },
    )
    createTables(database)
    return database
}
