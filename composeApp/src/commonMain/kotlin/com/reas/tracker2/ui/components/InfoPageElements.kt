package com.reas.tracker2.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tracker2.composeapp.generated.resources.Res
import tracker2.composeapp.generated.resources.more

@Composable
fun RowScope.InfoBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = modifier
            .weight(1.0F)
            .border(1.dp, Color.Gray, MaterialTheme.shapes.medium)
            .aspectRatio(1.0F),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            content()
        }
    }
}

@Composable
fun InfoChartHeader(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 10.dp),
        )
        Spacer(Modifier.weight(1.0F))
        AssistChip(
            onClick = onClick,
            label = { Text(stringResource(Res.string.more)) },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowRight,
                    stringResource(Res.string.more),
                )
            },
        )
    }
}
