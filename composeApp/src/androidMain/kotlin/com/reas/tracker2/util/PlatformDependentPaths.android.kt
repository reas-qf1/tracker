package com.reas.tracker2.util

import android.content.Context
import com.reas.tracker2.settings.dataStoreFileName
import io.github.oshai.kotlinlogging.KotlinLogging

private object Database {
    val logger = KotlinLogging.logger {}
}

class PlatformDependentPathsAndroid(
    val context: Context,
) : PlatformDependentPaths {
    override fun getPreferencesPath(): String = context.filesDir.resolve(dataStoreFileName).absolutePath

    override fun getDatabasePath(): String {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath("database")
        Database.logger.debug { "Database file: $dbFile" }
        return dbFile.absolutePath
    }
}
