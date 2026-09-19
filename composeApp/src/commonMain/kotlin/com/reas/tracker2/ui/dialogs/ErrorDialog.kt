package com.reas.tracker2.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reas.tracker2.ui.navigation.ApplicationState
import com.reas.tracker2.ui.navigation.Error

@Composable
fun ErrorDialog(arguments: Error, applicationState: ApplicationState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
            ).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Error", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.headlineLarge)
        Text(arguments.message, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { applicationState.goBack() },
            colors = ButtonDefaults.buttonColors(
                contentColor = MaterialTheme.colorScheme.onError,
                containerColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text("OK")
        }
    }
}
