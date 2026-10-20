package com.reas.tracker2.util

import com.reas.tracker2.shared.Scrobble
import kotlin.uuid.Uuid

interface NowPlayingNotificationManager {
    fun show(scrobble: Scrobble)

    fun showDefault()

    fun currentId(): Uuid?

    fun edit(scrobble: Scrobble) {
        if (currentId() == scrobble.id) {
            show(scrobble)
        }
    }

    fun delete(scrobble: Scrobble) {
        if (currentId() == scrobble.id) {
            showDefault()
        }
    }
}
