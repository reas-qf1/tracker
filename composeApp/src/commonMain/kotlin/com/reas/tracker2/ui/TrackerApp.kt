package com.reas.tracker2.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.reas.tracker2.buildConfig.IS_DEBUG
import com.reas.tracker2.ui.dialogs.ErrorDialog
import com.reas.tracker2.ui.dialogs.delete.DeleteDialog
import com.reas.tracker2.ui.navigation.*
import com.reas.tracker2.ui.screens.albuminfo.AlbumInfoScreen
import com.reas.tracker2.ui.screens.artistinfo.ArtistInfoScreen
import com.reas.tracker2.ui.screens.charts.ChartsScreen
import com.reas.tracker2.ui.screens.debug.DebugScreen
import com.reas.tracker2.ui.screens.history.HistoryScreen
import com.reas.tracker2.ui.screens.settings.SettingsScreen
import com.reas.tracker2.ui.screens.trackhistory.TrackHistoryScreen
import com.reas.tracker2.ui.screens.trackinfo.TrackInfoScreen
import com.reas.tracker2.ui.sheets.edit.EditBottomSheet
import com.reas.tracker2.ui.sheets.info.InfoBottomSheet
import com.reas.tracker2.ui.theme.TrackerTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import tracker2.composeapp.generated.resources.*

@Composable
fun TrackerApp(modifier: Modifier = Modifier) {
    val backStack = rememberBackStack(startRoute = History)
    val applicationState = rememberApplicationState(backStack)

    TrackerBackgroundProcesses(
        onError = { message -> applicationState.navigate(Error(message)) },
    )
    TrackerTheme {
        TrackerNavScaffold(
            applicationState,
            navigationItems = listOf(
                TrackerNavItem(
                    title = stringResource(Res.string.history),
                    icon = Icons.Filled.History,
                    destination = History,
                ),
                TrackerNavItem(
                    title = stringResource(Res.string.charts),
                    icon = Icons.Filled.Album,
                    destination = Charts(),
                ),
                TrackerNavItem(
                    title = stringResource(Res.string.settings),
                    icon = Icons.Filled.Settings,
                    destination = Settings,
                ),
            ).let {
                if (IS_DEBUG) {
                    it + TrackerNavItem(
                        title = "Debug",
                        icon = vectorResource(Res.drawable.wrench),
                        destination = Debug,
                    )
                } else {
                    it
                }
            },
            modifier = modifier,
        ) {
            entry<History> { screen ->
                HistoryScreen(screen, applicationState)
            }

            entry<TrackHistory> { screen ->
                TrackHistoryScreen(screen, applicationState)
            }

            entry<Charts> { screen ->
                ChartsScreen(screen, applicationState)
            }

            entry<Settings> { screen ->
                SettingsScreen(screen, applicationState)
            }

            entry<Debug> { screen ->
                DebugScreen(screen, applicationState)
            }

            entry<ArtistInfo> { screen ->
                ArtistInfoScreen(screen, applicationState)
            }

            entry<AlbumInfo> { screen ->
                AlbumInfoScreen(screen, applicationState)
            }

            entry<TrackInfo> { screen ->
                TrackInfoScreen(screen, applicationState)
            }

            bottomSheet<InfoBottomSheet> { screen ->
                InfoBottomSheet(screen, applicationState)
            }

            bottomSheet<EditBottomSheet> { screen ->
                EditBottomSheet(screen, applicationState)
            }

            dialog<Error> { screen ->
                ErrorDialog(screen, applicationState)
            }

            dialog<Delete> { screen ->
                DeleteDialog(screen, applicationState)
            }
        }
    }
}
