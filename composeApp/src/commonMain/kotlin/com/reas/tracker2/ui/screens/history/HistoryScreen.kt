package com.reas.tracker2.ui.screens.history

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reas.tracker2.ui.components.DividerWithText
import com.reas.tracker2.ui.components.HistoryEntry
import com.reas.tracker2.ui.components.PagedLazyColumn
import com.reas.tracker2.ui.components.ScrollToTopButton
import com.reas.tracker2.ui.navigation.*
import com.reas.tracker2.ui.rememberAsPagingItems
import com.reas.tracker2.ui.state
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(
    screen: History,
    applicationState: ApplicationState,
    modifier: Modifier = Modifier,
    viewModel: HistoryScreenViewModel = koinViewModel(),
) {
    val history = rememberAsPagingItems { viewModel.history }
    val isRefreshing by state(false)
    val refreshState = rememberPullToRefreshState()
    val scrollState = viewModel.scrollState

    ScrollToTopButton(screen.screenState, scrollState)
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
        PagedLazyColumn(scrollState, history, key = { scrobble -> scrobble.key() }) { _, entry ->
            when (entry) {
                is HistoryEntry.Play -> {
                    val scrobble = entry.play
                    HistoryEntry(
                        play = scrobble,
                        modifier = Modifier.padding(5.dp).height(84.dp).animateItem(),
                        imageUrl = { viewModel.getImageUrl(scrobble) },
                        onClick = {
                            applicationState.navigate(InfoBottomSheet(track = scrobble.metadata))
                        },
                        onDelete = {
                            applicationState.navigate(Delete(play = scrobble))
                        },
                        onEdit = {
                            applicationState.navigate(EditBottomSheet(play = scrobble))
                        },
                    )
                }

                is HistoryEntry.Separator -> {
                    DividerWithText(entry.text, modifier = Modifier.padding(5.dp).animateItem())
                }
            }
        }
    }
}
