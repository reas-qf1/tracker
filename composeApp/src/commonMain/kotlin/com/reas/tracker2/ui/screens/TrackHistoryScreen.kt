package com.reas.tracker2.ui.screens

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.itemKey
import com.reas.tracker2.shared.Play
import com.reas.tracker2.ui.components.ConfirmDialog
import com.reas.tracker2.ui.components.HistoryEntry
import com.reas.tracker2.ui.components.LazyColumnWithScrollButton
import com.reas.tracker2.ui.dialogs.EditDialog
import com.reas.tracker2.ui.navigation.ApplicationState
import com.reas.tracker2.ui.navigation.BottomSheetInfo
import com.reas.tracker2.ui.navigation.TrackHistory
import com.reas.tracker2.ui.rememberAsPagingItems
import com.reas.tracker2.ui.rememberAsState
import com.reas.tracker2.ui.state
import com.reas.tracker2.ui.viewmodels.TrackHistoryViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TrackHistoryScreen(
    arguments: TrackHistory,
    applicationState: ApplicationState,
    modifier: Modifier = Modifier,
    viewModel: TrackHistoryViewModel = koinViewModel(),
) {
    val track = arguments.track
    applicationState.setTitle("${track.artistsAsString} - ${track.name}")

    val trackPlays by rememberAsState { viewModel.trackPlays(track) }
    val history = rememberAsPagingItems { viewModel.history(track) }
    val scope = rememberCoroutineScope()
    var deletingScrobble by state<Play?>(null)
    var editingScrobble by state<Play?>(null)

    LazyColumnWithScrollButton(applicationState, modifier = modifier) {
        item(key = "header") {
            Text(
                "Plays: $trackPlays",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
            )
        }

        items(
            history.itemCount,
            key = history.itemKey { scrobble -> scrobble.key },
        ) { index ->
            val scrobble = history[index]
            scrobble?.let {
                HistoryEntry(
                    play = scrobble,
                    modifier = Modifier.padding(5.dp).height(84.dp),
                    imageUrl = { viewModel.getImageUrl(scrobble) },
                    onClick = {
                        applicationState.navigate(BottomSheetInfo(track = scrobble.metadata))
                    },
                    onDelete = { deletingScrobble = scrobble },
                    onEdit = { editingScrobble = scrobble },
                )
            }
        }
    }

    deletingScrobble?.let { scrobble ->
        ConfirmDialog(
            onConfirm = {
                scope.launch {
                    viewModel.delete(scrobble)
                }
            },
            onDismiss = { deletingScrobble = null },
        ) {
            Icon(Icons.Filled.Warning, "Warning", modifier = Modifier.size(120.dp))
            Text("Are you sure you want to delete this scrobble?")
        }
    }

    editingScrobble?.let { scrobble ->
        EditDialog(
            scrobble = scrobble,
            onSave = { track ->
                scope.launch {
                    viewModel.edit(scrobble, track)
                }
            },
            onDismiss = { editingScrobble = null },
        )
    }
}
