package com.reas.tracker2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.horizontalGradient
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reas.tracker2.ui.derivedState
import com.reas.tracker2.ui.navigation.ApplicationState
import kotlinx.coroutines.launch

@Composable
fun LazyColumnWithScrollButton(
    applicationState: ApplicationState,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    val showButton by derivedState { state.firstVisibleItemIndex > 0 }
    val scope = rememberCoroutineScope()
    applicationState.FloatingActionButton(
        visibleIf = showButton,
        onClick = {
            scope.launch {
                state.animateScrollToItem(0)
            }
        },
    ) {
        Icon(imageVector = Icons.Filled.ArrowUpward, contentDescription = "Scroll to top")
    }

    LazyColumn(state = state, modifier = modifier, verticalArrangement = verticalArrangement) {
        item(key = "_top") {
            Spacer(Modifier.height(2.dp))
        }

        content()

        if (showButton) {
            item(key = "_bottom") {
                EndIndicator(Modifier.height(75.dp))
            }
        }
    }
}

@Composable
fun EndIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier.fillMaxWidth().background(
                brush = Brush.composite(
                    verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    ),
                    horizontalGradient(
                        0.0f to Color.White.copy(alpha = 0.0f),
                        0.2f to Color.White,
                        0.8f to Color.White,
                        1.0f to Color.White.copy(alpha = 0.0f),
                    ),
                    blendMode = BlendMode.Modulate,
                ),
            ),
    )
}
