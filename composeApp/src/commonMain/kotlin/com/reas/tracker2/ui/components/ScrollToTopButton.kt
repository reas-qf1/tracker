package com.reas.tracker2.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.reas.tracker2.ui.derivedState
import com.reas.tracker2.ui.navigation.ScreenState
import kotlinx.coroutines.launch

@Composable
fun ScrollToTopButton(screenState: ScreenState, state: LazyListState) {
    val showButton by derivedState { state.firstVisibleItemIndex > 0 }
    val scope = rememberCoroutineScope()

    screenState.setFab(
        visibleIf = showButton,
        onClick = {
            scope.launch {
                state.animateScrollToItem(0, 0)
            }
        },
    ) {
        Icon(imageVector = Icons.Filled.ArrowUpward, contentDescription = "Scroll to top")
    }
}
