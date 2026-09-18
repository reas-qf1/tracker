package com.reas.tracker2.util

import com.reas.tracker2.shared.Event
import com.reas.tracker2.shared.Play

interface NowPlayingNotificationManager {
    fun show(event: Event)
    fun show(play: Play)
    fun showDefault()
}