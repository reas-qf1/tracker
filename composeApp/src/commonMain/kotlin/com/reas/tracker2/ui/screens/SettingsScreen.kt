package com.reas.tracker2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reas.tracker2.ui.navigation.ApplicationState
import com.reas.tracker2.ui.rememberAsState
import com.reas.tracker2.ui.viewmodels.SettingsScreenViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tracker2.composeapp.generated.resources.Res
import tracker2.composeapp.generated.resources.current_version
import tracker2.composeapp.generated.resources.enable_tracking
import tracker2.composeapp.generated.resources.settings

@Composable
fun SettingsScreen(
    applicationState: ApplicationState,
    modifier: Modifier = Modifier,
    viewModel: SettingsScreenViewModel = koinViewModel()
) {
    applicationState.setTitle(stringResource(Res.string.settings))
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.padding(horizontal = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val scrobblingEnabled by rememberAsState { viewModel.scrobblingEnabledFlow }
            Text(stringResource(Res.string.enable_tracking))
            Spacer(Modifier.weight(1.0F))
            Switch(
                checked = scrobblingEnabled,
                onCheckedChange = { viewModel.setScrobblingEnabled(it) }
            )
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        Text(stringResource(Res.string.current_version, "1.0"))
    }
}