package com.reas.tracker2.util

import com.reas.tracker2.settings.dataStoreFileName
import java.io.File

class PlatformDependentPathsDesktop : PlatformDependentPaths {
    override fun getPreferencesPath(): String =
        File(/*System.getProperty("java.io.tmpdir"), */dataStoreFileName).absolutePath

    override fun getDatabasePath(): String =
        File(/*System.getProperty("java.io.tmpdir"), */"client.db").absolutePath
}