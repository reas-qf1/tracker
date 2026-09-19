package com.reas.tracker2.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.reas.tracker2.buildConfig.IS_DEBUG
import com.reas.tracker2.ui.dialogs.ErrorDialog
import com.reas.tracker2.ui.dialogs.InfoBottomSheet
import com.reas.tracker2.ui.navigation.*
import com.reas.tracker2.ui.screens.*
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
            entry<History> {
                HistoryScreen(applicationState)
            }

            entry<TrackHistory> { arguments ->
                TrackHistoryScreen(arguments, applicationState)
            }

            entry<Charts> { arguments ->
                ChartsScreen(arguments, applicationState)
            }

            entry<Settings> {
                SettingsScreen(applicationState)
            }

            entry<Debug> {
                DebugScreen(applicationState)
            }

            entry<ArtistInfo> { arguments ->
                ArtistInfoScreen(arguments, applicationState)
            }

            entry<AlbumInfo> { arguments ->
                AlbumInfoScreen(arguments, applicationState)
            }

            entry<TrackInfo> { arguments ->
                TrackInfoScreen(arguments, applicationState)
            }

            dialog<BottomSheetInfo> { arguments ->
                InfoBottomSheet(arguments, applicationState)
            }

            dialog<Error> { arguments ->
                ErrorDialog(arguments, applicationState)
            }
        }
    }
}
