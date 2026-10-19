package com.reas.tracker2.util

import com.reas.tracker2.shared.Play
import kotlin.uuid.Uuid

interface NowPlayingNotificationManager {
    fun show(play: Play)

    fun showDefault()

    fun currentId(): Uuid?

    fun edit(play: Play) {
        if (currentId() == play.id) {
            show(play)
        }
    }

    fun delete(play: Play) {
        if (currentId() == play.id) {
            showDefault()
        }
    }
}
