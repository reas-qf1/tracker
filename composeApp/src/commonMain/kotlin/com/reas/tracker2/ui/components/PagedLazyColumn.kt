package com.reas.tracker2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.horizontalGradient
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.reas.tracker2.ui.derivedState

@Composable
fun <T : Any> PagedLazyColumn(
    state: LazyListState,
    items: LazyPagingItems<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    header: LazyListScope.() -> Unit = {},
    content: @Composable LazyItemScope.(Int, T) -> Unit,
) {
    val showBottom by derivedState { state.firstVisibleItemIndex > 0 }
    val actualScrollState by derivedState {
        if (items.itemCount == 0) {
            LazyListState()
        } else {
            state
        }
    }

    LazyColumn(state = actualScrollState, modifier = modifier, verticalArrangement = verticalArrangement) {
        item(key = "_top") {
            Spacer(Modifier.height(2.dp))
        }

        header()
        items(items.itemCount, key = items.itemKey(key = key)) { index ->
            val entry = items[index]
            entry?.let {
                content(index, entry)
            }
        }

        if (showBottom) {
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
