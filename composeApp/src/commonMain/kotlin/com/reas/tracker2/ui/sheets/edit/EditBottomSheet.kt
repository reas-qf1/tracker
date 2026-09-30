package com.reas.tracker2.ui.sheets.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reas.tracker2.shared.ArtistList
import com.reas.tracker2.shared.TrackWithAlbum
import com.reas.tracker2.ui.components.ConfirmRow
import com.reas.tracker2.ui.derivedState
import com.reas.tracker2.ui.navigation.ApplicationState
import com.reas.tracker2.ui.navigation.EditBottomSheet
import com.reas.tracker2.ui.state
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TextFieldWithRevertButton(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
) {
    val initialValue = remember { value }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = label,
            supportingText = supportingText,
            modifier = Modifier.weight(1.0F),
        )
        IconButton(
            enabled = value != initialValue,
            onClick = { onChange(initialValue) },
        ) {
            Icon(Icons.AutoMirrored.Filled.Undo, "Revert")
        }
    }
}

@Composable
fun EditBottomSheet(
    arguments: EditBottomSheet,
    applicationState: ApplicationState,
    modifier: Modifier = Modifier,
    viewModel: EditBottomSheetViewModel = koinViewModel(),
) {
    val scrobble = arguments.play
    var track by state(scrobble.track)
    var artists by state(scrobble.artistsAsString)
    var album by state(scrobble.album ?: "")
    var albumArtists by state(
        if (scrobble.albumArtistsAsString != null && scrobble.albumArtistsAsString != scrobble.artistsAsString) {
            scrobble.albumArtistsAsString!!
        } else {
            ""
        },
    )

    val canSave by derivedState { track.isNotEmpty() && artists.isNotEmpty() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextFieldWithRevertButton(
            value = track,
            onChange = { track = it },
            label = { Text("Track") },
        )
        TextFieldWithRevertButton(
            value = artists,
            onChange = { artists = it },
            label = { Text("Artist(s)") },
        )
        TextFieldWithRevertButton(
            value = album,
            onChange = { album = it },
            label = { Text("Album") },
        )
        TextFieldWithRevertButton(
            value = albumArtists,
            onChange = { albumArtists = it },
            label = { Text("Album artist(s)") },
            supportingText = { Text("Will be equal to track artist(s) if left empty") },
        )

        ConfirmRow(
            enabled = canSave,
            modifier = Modifier.padding(12.dp),
            onConfirm = {
                val track = TrackWithAlbum(
                    track = track,
                    artists = ArtistList(artists),
                    album = album.ifEmpty { null },
                    albumArtists = if (album.isEmpty()) null else ArtistList(albumArtists.ifEmpty { artists }),
                )
                viewModel.edit(scrobble, track)
            },
            onDismiss = { applicationState.goBack() },
        )
    }
}
