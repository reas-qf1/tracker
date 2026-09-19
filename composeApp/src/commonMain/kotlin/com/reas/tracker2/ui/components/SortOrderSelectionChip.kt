package com.reas.tracker2.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.reas.tracker2.ui.navigation.ChartSort
import org.jetbrains.compose.resources.stringResource

@Composable
fun SortOrderSelectionChip(value: ChartSort, setValue: (ChartSort) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        AssistChip(
            onClick = {
                setValue(
                    if (value == ChartSort.TIME) ChartSort.PLAYS else ChartSort.TIME,
                )
            },
            label = { Text(stringResource(value.label)) },
            leadingIcon = {
                Icon(
                    value.icon,
                    contentDescription = stringResource(value.label),
                    Modifier.size(AssistChipDefaults.IconSize),
                )
            },
        )
    }
}
