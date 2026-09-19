package com.reas.tracker2.android

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import io.github.oshai.kotlinlogging.KotlinLogging

private data class ChannelDescription(
    val id: String,
    val name: String,
    val importance: Int,
    val configuration: NotificationChannel.() -> Unit = {},
)

class NotificationWrapper(
    private val context: Context,
    private val notificationManager: NotificationManager,
) {
    companion object {
        const val PLAYING_ID = 0
    }

    private var nextId = 1
    private val channelDescriptions =
        listOf(
            ChannelDescription("0", "Now Playing", 2),
            ChannelDescription("1", "Daily Report", 1),
            ChannelDescription("2", "Sync Worker", 1),
        )

    private val channels = hashMapOf<String, String>()
    private val logger = KotlinLogging.logger {}

    init {
        channelDescriptions.forEach {
            channels[it.name] = it.id
        }
    }

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        notificationManager.notificationChannels.forEach { channel ->
            notificationManager.deleteNotificationChannel(channel.id)
        }
        channelDescriptions.forEach {
            val channel = NotificationChannel(it.id, it.name, it.importance).apply(it.configuration)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun reserveId() = nextId++

    fun notification(
        channel: String,
        params: NotificationBuilder,
    ): Notification =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification
                .Builder(context, channels[channel])
                .apply {
                    params(context)
                }.build()
        } else {
            Notification
                .Builder(context)
                .apply {
                    params(context)
                }.build()
        }

    fun show(
        channel: String,
        id: Int? = null,
        params: NotificationBuilder,
    ): Int {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            logger.error { "Notifications permission not granted" }
        }
        val notificationId = id ?: nextId++
        val notification = notification(channel, params)
        notificationManager.notify(notificationId, notification)
        return notificationId
    }

    fun hide(id: Int) {
        notificationManager.cancel(id)
    }
}

typealias NotificationBuilder = Notification.Builder.(Context) -> Unit
