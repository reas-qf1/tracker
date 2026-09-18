package com.reas.tracker2.util

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import com.reas.tracker2.MainActivity
import com.reas.tracker2.R
import com.reas.tracker2.android.NotificationBuilder
import com.reas.tracker2.android.NotificationWrapper
import com.reas.tracker2.shared.Event
import com.reas.tracker2.shared.EventInfo
import com.reas.tracker2.shared.Play
import com.reas.tracker2.shared.TrackWithAlbum

class NowPlayingNotificationManagerAndroid(private val notificationManager: NotificationWrapper) : NowPlayingNotificationManager {
    override fun show(event: Event) {
        updateNotification(notificationBuilder(event.metadata, event.info))
    }

    override fun show(play: Play) {
        updateNotification(notificationBuilder(play.metadata, play.lastEvent))
    }

    override fun showDefault() {
        updateNotification(defaultNotificationBuilder)
    }

    private fun notificationBuilder(metadata: TrackWithAlbum, event: EventInfo): NotificationBuilder =
        if (event.isPlaying) { context ->
            setContentTitle(metadata.name)
            setContentText(metadata.artistsAsString)
            setSmallIcon(R.drawable.ic_stat_name)
            setShowWhen(false)

            val resultIntent = Intent(context, MainActivity::class.java)
            val resultPendingIntent =
                TaskStackBuilder.create(context).run {
                    addNextIntentWithParentStack(resultIntent)
                    getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            setContentIntent(resultPendingIntent)
        } else defaultNotificationBuilder

    private val defaultNotificationBuilder: NotificationBuilder = {
        setContentTitle("Nothing is playing")
        setSmallIcon(R.drawable.ic_stat_name)
        setShowWhen(false)
    }

    private fun updateNotification(builder: NotificationBuilder) {
        notificationManager.show("Now Playing", NotificationWrapper.PLAYING_ID, builder)
    }
}