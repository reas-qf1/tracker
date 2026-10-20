package com.reas.tracker2.util

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import com.reas.tracker2.MainActivity
import com.reas.tracker2.R
import com.reas.tracker2.android.NotificationBuilder
import com.reas.tracker2.android.NotificationWrapper
import com.reas.tracker2.shared.Play
import com.reas.tracker2.shared.TrackWithAlbum
import kotlin.uuid.Uuid

class NowPlayingNotificationManagerAndroid(
    private val notificationManager: NotificationWrapper,
) : NowPlayingNotificationManager {
    private var currentId: Uuid? = null

    override fun show(play: Play) {
        updateNotification(if (play.lastPlaying) play.metadata else null)
        currentId = play.id
    }

    override fun showDefault() {
        updateNotification(null)
        currentId = null
    }

    override fun currentId(): Uuid? = currentId

    private fun updateNotification(metadata: TrackWithAlbum?) {
        val builder: NotificationBuilder = { context ->
            if (metadata != null) {
                setContentTitle(metadata.name)
                setContentText(metadata.artistsAsString)
            } else {
                setContentTitle("Nothing is playing")
            }
            setSmallIcon(R.drawable.ic_stat_name)
            setShowWhen(false)
            setOngoing(true)

            val resultIntent = Intent(context, MainActivity::class.java)
            val resultPendingIntent =
                TaskStackBuilder.create(context).run {
                    addNextIntentWithParentStack(resultIntent)
                    getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                }
            setContentIntent(resultPendingIntent)
        }
        notificationManager.show("Now Playing", NotificationWrapper.PLAYING_ID, builder)
    }
}
