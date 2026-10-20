package com.reas.tracker2.util

import com.reas.tracker2.buildConfig.LASTFM_API_KEY

class SecretManager {
    // TODO: get api key from settings if not defined in gradle properties
    // Did you mean Gradle the build tool?     Replace with 'Gradle'
    val lastfmApiKey: String?
        get() = LASTFM_API_KEY
}
