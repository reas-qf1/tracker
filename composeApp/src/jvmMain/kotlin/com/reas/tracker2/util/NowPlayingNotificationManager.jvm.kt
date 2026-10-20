package com.reas.tracker2.util

import com.reas.tracker2.shared.Scrobble
import kotlin.uuid.Uuid

class NowPlayingNotificationManagerDesktop : NowPlayingNotificationManager {
    override fun show(scrobble: Scrobble) {}

    override fun showDefault() {}

    override fun currentId(): Uuid? = null
}
