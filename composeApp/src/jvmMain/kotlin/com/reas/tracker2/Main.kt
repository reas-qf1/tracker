package com.reas.tracker2

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.reas.tracker2.ui.TrackerApp
import com.skydoves.snitcher.Snitcher
import com.skydoves.snitcher.install
import com.skydoves.snitcher.ui.SnitcherTraceWindow
import org.koin.core.context.GlobalContext.startKoin

fun main() {
    startKoin {
        printLogger()
        modules(sharedModule, platformModule)
    }
    Snitcher.install()
    Snitcher.isDebuggable = true

    application {
        SnitcherTraceWindow()
        Window(
            onCloseRequest = ::exitApplication,
            title = "Tracker2",
        ) {
            TrackerApp(modifier = Modifier.fillMaxSize())
        }
    }
}
