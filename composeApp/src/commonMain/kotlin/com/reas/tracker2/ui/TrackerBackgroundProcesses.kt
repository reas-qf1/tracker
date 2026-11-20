package com.reas.tracker2.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.reas.tracker2.internalsServer.LocalServer
import org.koin.compose.koinInject

@Composable
fun TrackerBackgroundProcesses(onError: suspend (String) -> Unit) {
    val server: LocalServer = koinInject()

    // plugging holes
    LaunchedEffect(Unit) {
        server.holePluggerLoop()
    }
}
