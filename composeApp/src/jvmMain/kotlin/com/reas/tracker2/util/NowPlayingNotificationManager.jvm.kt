package com.reas.tracker2.util

import com.reas.tracker2.shared.Event
import com.reas.tracker2.shared.Play

class NowPlayingNotificationManagerDesktop : NowPlayingNotificationManager {
    override fun show(event: Event) {}
    override fun show(play: Play) {}
    override fun showDefault() {}
}