package com.reas.tracker2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reas.tracker2.ui.components.ChartTypeSelectionChip
import com.reas.tracker2.ui.components.LazyDoubleChartColumn
import com.reas.tracker2.ui.components.SortOrderSelectionChip
import com.reas.tracker2.ui.navigation.ApplicationState
import com.reas.tracker2.ui.navigation.ChartSort
import com.reas.tracker2.ui.navigation.Charts
import com.reas.tracker2.ui.rememberAsPagingItems
import com.reas.tracker2.ui.viewmodels.ChartsScreenViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tracker2.composeapp.generated.resources.Res
import tracker2.composeapp.generated.resources.charts

@Composable
fun ChartsScreen(
    arguments: Charts,
    applicationState: ApplicationState,
    modifier: Modifier = Modifier,
    viewModel: ChartsScreenViewModel = koinViewModel(),
) {
    val chartType = arguments.type
    val sort by viewModel.sort().collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val infoTime = rememberAsPagingItems { viewModel.getInfo(arguments, ChartSort.TIME) }
    val infoPlays = rememberAsPagingItems { viewModel.getInfo(arguments, ChartSort.PLAYS) }

    applicationState.setTitle(stringResource(Res.string.charts))
    Column(modifier = modifier.padding(horizontal = 5.dp)) {
        Row {
            ChartTypeSelectionChip(chartType, { applicationState.navigate(arguments.copy(type = it)) })
            Spacer(Modifier.width(10.dp))
            SortOrderSelectionChip(sort, { scope.launch { viewModel.setSort(it) } })
        }

        LazyDoubleChartColumn(
            applicationState,
            sort.byTime,
            infoTime,
            infoPlays,
            onClick = { entry -> applicationState.navigate(entry.bottomSheetInfo) },
        )
    }
}
