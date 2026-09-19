package com.reas.tracker2.ui.screens

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.itemKey
import com.reas.tracker2.shared.Play
import com.reas.tracker2.ui.components.ConfirmDialog
import com.reas.tracker2.ui.components.DividerWithText
import com.reas.tracker2.ui.components.HistoryEntry
import com.reas.tracker2.ui.components.LazyColumnWithScrollButton
import com.reas.tracker2.ui.dialogs.EditDialog
import com.reas.tracker2.ui.navigation.ApplicationState
import com.reas.tracker2.ui.navigation.BottomSheetInfo
import com.reas.tracker2.ui.rememberAsPagingItems
import com.reas.tracker2.ui.state
import com.reas.tracker2.ui.viewmodels.HistoryEntry
import com.reas.tracker2.ui.viewmodels.HistoryScreenViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tracker2.composeapp.generated.resources.Res
import tracker2.composeapp.generated.resources.history

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(applicationState: ApplicationState, modifier: Modifier = Modifier, viewModel: HistoryScreenViewModel = koinViewModel()) {
    applicationState.setTitle(stringResource(Res.string.history))

    val scope = rememberCoroutineScope()
    var deletingScrobble by state<Play?>(null)
    var editingScrobble by state<Play?>(null)
    val history = rememberAsPagingItems { viewModel.history }
    val isRefreshing by state(false)
    val refreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        state = refreshState,
        onRefresh = { history.refresh() },
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                refreshState,
                isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
        modifier = modifier,
    ) {
        LazyColumnWithScrollButton(applicationState) {
            items(
                history.itemCount,
                key = history.itemKey { scrobble -> scrobble.key() },
            ) { index ->
                val entry = history[index]
                entry?.let {
                    when (entry) {
                        is HistoryEntry.Play -> {
                            val scrobble = entry.play
                            HistoryEntry(
                                play = scrobble,
                                modifier = Modifier.padding(5.dp).height(84.dp).animateItem(),
                                imageUrl = { viewModel.getImageUrl(scrobble) },
                                onClick = {
                                    applicationState.navigate(BottomSheetInfo(track = scrobble.metadata))
                                },
                                onDelete = { deletingScrobble = scrobble },
                                onEdit = { editingScrobble = scrobble },
                            )
                        }

                        is HistoryEntry.Separator -> {
                            DividerWithText(entry.text, modifier = Modifier.padding(5.dp).animateItem())
                        }
                    }
                }
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
