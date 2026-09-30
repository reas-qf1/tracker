package com.reas.tracker2.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import kotlinx.serialization.serializer

class ApplicationStateData(
    val backStack: NavBackStack<Route>,
    val snackbarHostState: SnackbarHostState,
)

class ScreenState(
    val title: MutableState<String>,
    val isFloatingButtonVisible: MutableState<Boolean>,
    val floatingButtonContents: MutableState<(@Composable () -> Unit)>,
    val floatingButtonOnClick: MutableState<() -> Unit>,
) {
    fun setFab(visibleIf: Boolean, onClick: () -> Unit = {}, content: @Composable (() -> Unit)) {
        floatingButtonContents.value = content
        floatingButtonOnClick.value = onClick
        isFloatingButtonVisible.value = visibleIf
    }
}

class ApplicationState(
    val state: ApplicationStateData,
) {
    fun currentScreen(): ScreenRoute = state.backStack.last { it is ScreenRoute } as ScreenRoute

    fun navigate(route: Route) {
        if (state.backStack.last() is DialogRoute) {
            state.backStack.removeLastOrNull()
        }
        state.backStack.add(route)
    }

    fun goBack() {
        if (state.backStack.size > 1) {
            state.backStack.removeLastOrNull()
        }
    }

    fun canNavigateBack() = state.backStack.filterIsInstance<ScreenRoute>().size > 1

    fun snackbarHostState() = state.snackbarHostState

    suspend fun snackbar(
        message: String,
        actionLabel: String?,
        withDismissAction: Boolean,
        duration: SnackbarDuration,
        onAction: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        val result = state.snackbarHostState.showSnackbar(message, actionLabel, withDismissAction, duration)
        when (result) {
            SnackbarResult.ActionPerformed -> onAction()
            SnackbarResult.Dismissed -> onDismiss()
        }
    }

    @Composable
    fun ActionButton(modifier: Modifier = Modifier) {
        val state = currentScreen().screenState
        AnimatedVisibility(
            visible = state.isFloatingButtonVisible.value,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = modifier,
        ) {
            FloatingActionButton(
                onClick = state.floatingButtonOnClick.value,
                content = state.floatingButtonContents.value,
            )
        }
    }
}

@Composable
fun rememberBackStack(startRoute: Route) = rememberSerializable(serializer = serializer()) { NavBackStack(startRoute) }

@Composable
fun rememberApplicationState(backStack: NavBackStack<Route>): ApplicationState {
    val snackbarHostState = remember { SnackbarHostState() }

    return remember {
        ApplicationState(
            ApplicationStateData(
                backStack = backStack,
                snackbarHostState = snackbarHostState,
            ),
        )
    }
}

fun screenState(defaultTitle: String): ScreenState =
    ScreenState(
        title = mutableStateOf(defaultTitle),
        isFloatingButtonVisible = mutableStateOf(false),
        floatingButtonContents = mutableStateOf({}),
        floatingButtonOnClick = mutableStateOf({}),
    )
