package com.reas.tracker2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.reas.tracker2.ui.navigation.ApplicationState
import com.reas.tracker2.ui.rememberAsState
import com.reas.tracker2.ui.viewmodels.DebugScreenViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DebugScreen(
    applicationState: ApplicationState,
    modifier: Modifier = Modifier,
    viewModel: DebugScreenViewModel = koinViewModel()
) {
    val playCount by rememberAsState { viewModel.playCount }
    val mediaEventLog by rememberAsState { viewModel.mediaEventLog }

    val verticalScrollState = rememberScrollState()
    LaunchedEffect(mediaEventLog) {
        if (!verticalScrollState.canScrollForward)
            verticalScrollState.scrollTo(Int.MAX_VALUE)
    }

    applicationState.setTitle("Debug")
    Column(modifier = modifier) {
        Text("Total plays: $playCount")
        Text("Event log:")
        Text(
            text = mediaEventLog,
            fontSize = MaterialTheme.typography.bodySmall.fontSize,
            fontFamily = FontFamily.Monospace,
            lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.2,
            modifier = Modifier.fillMaxSize()
                .padding(10.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium)
                .verticalScroll(verticalScrollState)
                .horizontalScroll(rememberScrollState())
                .padding(10.dp)
        )
    }
}