package com.reas.tracker2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.reas.tracker2.ui.components.*
import com.reas.tracker2.ui.navigation.ApplicationState
import com.reas.tracker2.ui.navigation.ArtistInfo
import com.reas.tracker2.ui.rememberAsState
import com.reas.tracker2.ui.viewmodels.ArtistInfoScreenViewModel
import com.reas.tracker2.util.toDisplayString
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tracker2.composeapp.generated.resources.*

@Composable
fun ArtistInfoScreen(
    arguments: ArtistInfo,
    applicationState: ApplicationState,
    modifier: Modifier = Modifier,
    viewModel: ArtistInfoScreenViewModel = koinViewModel(),
) {
    val artist = arguments.artist
    val sort by viewModel.sort().collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val period = TimePeriod.ALLTIME

    val plays by rememberAsState { viewModel.plays(artist, period) }
    val timePlayed by rememberAsState { viewModel.timePlayed(artist, period) }
    val playsAsString = if (plays == -1) "..." else plays.toString()
    val timePlayedAsString = timePlayed.toDisplayString()
    val timeRank by rememberAsState { viewModel.rank(artist, period) }
    val playRank by rememberAsState { viewModel.playRank(artist, period) }

    val timeAlbums by rememberAsState { viewModel.topAlbums(artist, period) }
    val playAlbums by rememberAsState { viewModel.topAlbumsByPlayCount(artist, period) }
    val timeTracks by rememberAsState { viewModel.topTracks(artist, period) }
    val playTracks by rememberAsState { viewModel.topTracksByPlayCount(artist, period) }

    applicationState.setTitle(artist.name)
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
                modifier = Modifier.height(125.dp),
                alignment = Alignment.CenterVertically,
            ) {
                AutosizingText(
                    artist.name,
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.weight(1.0F),
                )
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
                    AutosizingText(timePlayedAsString, style = MaterialTheme.typography.headlineSmall)
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

            Spacer(Modifier.height(5.dp))
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            InfoChartHeader(
                stringResource(Res.string.top_albums),
                onClick = {},
                modifier = Modifier.padding(top = 5.dp),
            )
            DoubleChartColumn(
                sort.byTime,
                timeAlbums,
                playAlbums,
                onClick = { entry -> applicationState.navigate(entry.bottomSheetInfo) },
            )

            Spacer(Modifier.height(5.dp))
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            InfoChartHeader(
                stringResource(Res.string.top_tracks),
                onClick = {},
                modifier = Modifier.padding(top = 5.dp),
            )
            DoubleChartColumn(
                sort.byTime,
                timeTracks,
                playTracks,
                onClick = { entry -> applicationState.navigate(entry.bottomSheetInfo) },
            )

            Spacer(Modifier.height(5.dp))
        }
    }
}
