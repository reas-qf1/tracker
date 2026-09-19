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
        repository.getNowPlayingTracks().collect { plays ->
            holePlugger.cancelAll()
            holePlugger.register(plays)
        }
    }
    LaunchedEffect(Unit) {
        holePlugger.collectPlays { play ->
            repository.insertPlay(play)
        }
    }
}
