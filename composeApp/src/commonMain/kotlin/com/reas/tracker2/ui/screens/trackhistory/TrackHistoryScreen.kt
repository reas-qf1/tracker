package com.reas.tracker2.ui.screens.trackhistory

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reas.tracker2.ui.components.HistoryEntry
import com.reas.tracker2.ui.components.PagedLazyColumn
import com.reas.tracker2.ui.components.ScrollToTopButton
import com.reas.tracker2.ui.navigation.*
import com.reas.tracker2.ui.rememberAsPagingItems
import com.reas.tracker2.ui.rememberAsState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TrackHistoryScreen(
    screen: TrackHistory,
    applicationState: ApplicationState,
    modifier: Modifier = Modifier,
    viewModel: TrackHistoryViewModel = koinViewModel(),
) {
    val track = screen.track

    val trackPlays by rememberAsState { viewModel.trackPlays(track) }
    val history = rememberAsPagingItems { viewModel.history(track) }
    val scrollState = viewModel.scrollState

    ScrollToTopButton(screen.screenState, scrollState)
    PagedLazyColumn(
        state = scrollState,
        items = history,
        key = { scrobble -> scrobble.key },
        header = {
            item(key = "header") {
                Text(
                    "Plays: $trackPlays",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                )
            }
        },
        modifier = modifier,
    ) { _, scrobble ->
        HistoryEntry(
            play = scrobble,
            modifier = Modifier.padding(5.dp).height(84.dp),
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
}
