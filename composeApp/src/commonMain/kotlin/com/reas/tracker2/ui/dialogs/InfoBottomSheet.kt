package com.reas.tracker2.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reas.tracker2.buildConfig.IS_DESKTOP
import com.reas.tracker2.ui.components.ScrollablePages
import com.reas.tracker2.ui.navigation.*
import com.reas.tracker2.ui.rememberAsState
import com.reas.tracker2.ui.state
import com.reas.tracker2.ui.viewmodels.InfoBottomSheetsViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tracker2.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(
    arguments: BottomSheetInfo,
    applicationState: ApplicationState,
    modifier: Modifier = Modifier,
    viewModel: InfoBottomSheetsViewModel = koinViewModel(),
) {
    val track = arguments.track?.name
    val album = arguments.track?.album ?: arguments.album?.name
    val artists = arguments.track?.artists ?: arguments.album?.artists ?: listOf(arguments.artist!!)
    val albumArtists = arguments.track?.albumArtists ?: arguments.album?.artists

    val trackO = arguments.track
    val albumO = arguments.track?.asAlbum ?: arguments.album

    ModalBottomSheet(
        onDismissRequest = { applicationState.goBack() },
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = if (IS_DESKTOP) 8.dp else 0.dp)) {
            track?.let {
                val trackPlays by rememberAsState { viewModel.trackPlays(trackO) }
                val trackTimePlayed by rememberAsState { viewModel.trackTimePlayed(trackO) }
                HistoryBottomSheetComponent(
                    icon = Icons.Filled.MusicNote,
                    iconDescription = stringResource(Res.string.track),
                    header = track,
                    buttonContents = listOf(
                        Res.string.track_plays to trackPlays,
                        Res.string.time_listened to trackTimePlayed,
                    ),
                    onMainButton = {
                        applicationState.navigate(TrackHistory(trackO))
                    },
                    onMore = {
                        applicationState.navigate(TrackInfo(trackO))
                    },
                )
                BottomSheetSpacer()
            }

            ScrollablePages(artists.size) { i ->
                val artistPlays by rememberAsState { viewModel.artistPlays(artists[i]) }
                val artistTimePlayed by rememberAsState { viewModel.artistTimePlayed(artists[i]) }
                HistoryBottomSheetComponent(
                    icon = Icons.Filled.Person,
                    iconDescription = stringResource(Res.string.artist),
                    header = artists[i].name,
                    buttonContents = listOf(
                        Res.string.artist_plays to artistPlays,
                        Res.string.time_listened to artistTimePlayed,
                    ),
                    onMore = {
                        applicationState.navigate(ArtistInfo(artists[i]))
                    },
                )
            }

            album?.let {
                albumO!!
                val differentArtists = albumArtists != null && artists.toSet() != albumArtists.toSet()
                val albumPlays by rememberAsState { viewModel.albumPlays(albumO) }
                val albumTimePlayed by rememberAsState { viewModel.albumTimePlayed(albumO) }
                BottomSheetSpacer()
                HistoryBottomSheetComponent(
                    icon = Icons.Filled.Album,
                    iconDescription = stringResource(Res.string.album),
                    header = album,
                    buttonContents = listOf(
                        Res.string.album_plays to albumPlays,
                        Res.string.time_listened to albumTimePlayed,
                    ),
                    onMore = {
                        applicationState.navigate(AlbumInfo(albumO))
                    },
                    icon2 = if (differentArtists) Icons.Filled.Person else null,
                    iconDescription2 = if (differentArtists) "Album artist" else null,
                    subtitle = if (differentArtists) albumArtists.raw else null,
                )
            }
        }
    }
}

@Composable
private fun BottomSheetSpacer() {
    Spacer(Modifier.height(10.dp))
    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun HistoryBottomSheetComponent(
    icon: ImageVector,
    iconDescription: String,
    header: String,
    buttonContents: List<Pair<StringResource, String>>,
    onMore: () -> Unit,
    onMainButton: () -> Unit = onMore,
    icon2: ImageVector? = null,
    iconDescription2: String? = null,
    subtitle: String? = null,
) {
    var expanded by state(false)

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = { expanded = !expanded })
                .padding(5.dp),
        ) {
            Icon(icon, iconDescription, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(5.dp))
            Text(
                header,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        icon2?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = { expanded = !expanded }),
            ) {
                Spacer(Modifier.width(5.dp))
                Icon(icon2, iconDescription2!!, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(5.dp))
                Text(
                    subtitle!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(5.dp))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    MaterialTheme.colorScheme.secondary,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                contentPadding = PaddingValues(0.dp),
                onClick = onMainButton,
                modifier = Modifier
                    .weight(2.0F)
                    .padding(5.dp)
                    .height(100.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    buttonContents.forEach { (line1, line2) ->
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                stringResource(line1),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                line2,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            Button(
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    MaterialTheme.colorScheme.primary,
                ),
                contentPadding = PaddingValues(0.dp),
                onClick = onMore,
                modifier = Modifier
                    .weight(1.0F)
                    .padding(5.dp)
                    .height(100.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "See more")
                    Text("More info", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
