package com.reas.tracker2.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.reas.tracker2.ui.navigation.ChartType
import com.reas.tracker2.ui.state
import org.jetbrains.compose.resources.stringResource
import kotlin.enums.enumEntries

@Composable
fun ChartTypeSelectionChip(value: ChartType, setValue: (ChartType) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        var expanded by state(false)
        AssistChip(
            onClick = { expanded = !expanded },
            label = { Text(stringResource(value.label)) },
            leadingIcon = {
                Icon(
                    value.icon,
                    contentDescription = stringResource(value.label),
                    Modifier.size(AssistChipDefaults.IconSize),
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            enumEntries<ChartType>().forEach { entry ->
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            entry.icon,
                            contentDescription = stringResource(entry.label),
                            Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                    text = { Text(stringResource(entry.label)) },
                    onClick = {
                        setValue(entry)
                        expanded = false
                    },
                )
            }
        }
    }
}
