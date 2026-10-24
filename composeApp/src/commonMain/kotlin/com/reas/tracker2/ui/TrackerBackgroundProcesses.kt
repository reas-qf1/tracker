package com.reas.tracker2.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.reas.tracker2.database.Repository
import com.reas.tracker2.shared.HolePlugger
import org.koin.compose.koinInject

@Composable
fun TrackerBackgroundProcesses(onError: suspend (String) -> Unit) {
    val repository: Repository = koinInject()
    val holePlugger: HolePlugger = koinInject()

    // plugging holes
    LaunchedEffect(Unit) {
        repository.getNowPlaying().collect { scrobbles ->
            holePlugger.cancelAll()
            holePlugger.register(scrobbles)
        }
    }
}
