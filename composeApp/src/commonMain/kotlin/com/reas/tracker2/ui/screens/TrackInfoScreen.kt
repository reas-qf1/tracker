package com.reas.tracker2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reas.tracker2.shared.TimePeriod
import com.reas.tracker2.ui.components.AutosizingText
import com.reas.tracker2.ui.components.InfoBox
import com.reas.tracker2.ui.components.ListEntryWithImage
import com.reas.tracker2.ui.components.SortOrderSelectionChip
import com.reas.tracker2.ui.navigation.ApplicationState
import com.reas.tracker2.ui.navigation.TrackInfo
import com.reas.tracker2.ui.rememberAsState
import com.reas.tracker2.ui.viewmodels.TrackInfoScreenViewModel
import com.reas.tracker2.util.toDisplayString
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tracker2.composeapp.generated.resources.*

@Composable
fun TrackInfoScreen(
    arguments: TrackInfo,
    applicationState: ApplicationState,
    modifier: Modifier = Modifier,
    viewModel: TrackInfoScreenViewModel = koinViewModel(),
) {
    val track = arguments.track
    val sort by viewModel.sort().collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val period = TimePeriod.ALLTIME

    val plays by rememberAsState { viewModel.plays(track, period) }
    val timePlayed by rememberAsState { viewModel.timePlayed(track, period) }
    val playsAsString = if (plays == -1) "..." else plays.toString()
    val timePlayedAsString = timePlayed.toDisplayString()
    val timeRank by rememberAsState { viewModel.rank(track, period) }
    val playRank by rememberAsState { viewModel.playRank(track, period) }

    applicationState.setTitle("${track.artistsAsString} - ${track.name}")
    Column(
        modifier = modifier.padding(top = 5.dp, start = 5.dp, end = 5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        SortOrderSelectionChip(sort, { scope.launch { viewModel.setSort(it) } })
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            ListEntryWithImage(
                url = { viewModel.getTrackImageUrl(track) },
                modifier = Modifier.height(125.dp),
                alignment = Alignment.CenterVertically,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1.0F),
                ) {
                    AutosizingText(track.name, style = MaterialTheme.typography.displaySmall)
                    track.artists.forEach { artist ->
                        AutosizingText(
                            artist.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    track.album?.let {
                        AutosizingText(
                            track.album!!,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(5.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InfoBox {
                    AutosizingText(playsAsString, style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(Res.string.plays).lowercase(), color = MaterialTheme.colorScheme.secondary)
                }
                InfoBox {
                    AutosizingText(
                        timePlayedAsString,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(stringResource(Res.string.time_played).lowercase(), color = MaterialTheme.colorScheme.secondary)
                }
                InfoBox {
                    if (sort.byTime) {
                        AutosizingText(timeRank, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            stringResource(Res.string.in_charts_by_time),
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        AutosizingText(playRank, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            stringResource(Res.string.in_charts_by_plays),
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
