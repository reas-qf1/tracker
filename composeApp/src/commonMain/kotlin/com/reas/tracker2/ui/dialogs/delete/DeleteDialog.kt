package com.reas.tracker2.ui.dialogs.delete

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reas.tracker2.ui.components.ConfirmDialog
import com.reas.tracker2.ui.navigation.ApplicationState
import com.reas.tracker2.ui.navigation.Delete
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeleteDialog(
    screen: Delete,
    applicationState: ApplicationState,
    modifier: Modifier = Modifier,
    viewModel: DeleteDialogViewModel = koinViewModel(),
) {
    val scrobble = screen.play
    ConfirmDialog(
        onConfirm = { viewModel.delete(scrobble) },
        onDismiss = { applicationState.goBack() },
        modifier = modifier,
    ) {
        Icon(Icons.Filled.Warning, "Warning", modifier = Modifier.size(120.dp))
        Text("Are you sure you want to delete this scrobble?")
    }
}
