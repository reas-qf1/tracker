package com.reas.tracker2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.reas.tracker2.android.DailyReportWorker
import com.reas.tracker2.android.NotificationWrapper
import com.reas.tracker2.ui.TrackerApp
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val notif: NotificationWrapper by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        notif.createChannels()

        DailyReportWorker.start(this)

        setContent {
            TrackerApp(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
