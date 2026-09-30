package com.reas.tracker2.ui.screens.charts

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reas.tracker2.ui.components.ChartTypeSelectionChip
import com.reas.tracker2.ui.components.LazyDoubleChartColumn
import com.reas.tracker2.ui.components.ScrollToTopButton
import com.reas.tracker2.ui.components.SortOrderSelectionChip
import com.reas.tracker2.ui.navigation.ApplicationState
import com.reas.tracker2.ui.navigation.ChartSort
import com.reas.tracker2.ui.navigation.Charts
import com.reas.tracker2.ui.rememberAsPagingItems
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChartsScreen(
    screen: Charts,
    applicationState: ApplicationState,
    modifier: Modifier = Modifier,
    viewModel: ChartsScreenViewModel = koinViewModel(),
) {
    val chartType = screen.type
    val sort by viewModel.sort().collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val scrollState = viewModel.scrollState

    val infoTime = rememberAsPagingItems { viewModel.getInfo(screen, ChartSort.TIME) }
    val infoPlays = rememberAsPagingItems { viewModel.getInfo(screen, ChartSort.PLAYS) }

    ScrollToTopButton(screen.screenState, scrollState)
    Column(modifier = modifier.padding(horizontal = 5.dp)) {
        Row {
            ChartTypeSelectionChip(chartType, { applicationState.navigate(screen.copy(type = it)) })
            Spacer(Modifier.width(10.dp))
            SortOrderSelectionChip(sort, { scope.launch { viewModel.setSort(it) } })
        }

        LazyDoubleChartColumn(
            scrollState,
            sort.byTime,
            infoTime,
            infoPlays,
            onClick = { entry -> applicationState.navigate(entry.infoBottomSheet) },
        )
    }
}
