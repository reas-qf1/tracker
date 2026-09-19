package com.reas.tracker2.util

interface PlatformDependentPaths {
    fun getPreferencesPath(): String

    fun getDatabasePath(): String
}
