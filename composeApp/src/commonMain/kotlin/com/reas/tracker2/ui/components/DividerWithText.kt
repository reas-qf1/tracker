package com.reas.tracker2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DividerWithText(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        HorizontalDivider(Modifier.weight(1f), DividerDefaults.Thickness, DividerDefaults.color)
        Text(text, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
        HorizontalDivider(Modifier.weight(1f), DividerDefaults.Thickness, DividerDefaults.color)
    }
}
