package com.reas.tracker2.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.*
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.DialogSceneStrategy.Companion.DialogKey
import androidx.navigation3.ui.NavDisplay
import com.reas.tracker2.ui.derivedState

class CustomNavEntryDecorator<T : Any>(
    appState: ApplicationState,
) : NavEntryDecorator<T>(
        decorate = { entry ->
            if (!entry.metadata.contains(DialogKey)) {
                appState.FloatingActionButton(visibleIf = false) {}
            }
            entry.Content()
        },
        onPop = { },
    )

inline fun <reified T : NavKey> EntryProviderScope<NavKey>.dialog(noinline content: @Composable (T) -> Unit) =
    entry<T>(metadata = DialogSceneStrategy.dialog(), content = content)

data class TrackerNavItem(
    val title: String,
    val icon: ImageVector,
    val destination: Route,
)

@Composable
fun TrackerNavScaffold(
    applicationState: TrackerApplicationState,
    modifier: Modifier = Modifier,
    navigationItems: List<TrackerNavItem>,
    entries: EntryProviderScope<NavKey>.() -> Unit,
) {
    val canNavigateBack by derivedState { applicationState.canNavigateBack() }
    val currentTab by derivedState { applicationState.currentTab() }

    val navLayoutType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
    val scrollBehavior =
        if (navLayoutType == NavigationSuiteType.ShortNavigationBarMedium) {
            TopAppBarDefaults.enterAlwaysScrollBehavior()
        } else {
            TopAppBarDefaults.pinnedScrollBehavior()
        }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = applicationState.snackbarHostState())
        },
        floatingActionButton = {
            if (navLayoutType == NavigationSuiteType.WideNavigationRailCollapsed) {
                applicationState.ActionButton()
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        applicationState.getTitle(),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { applicationState.goBack() },
                        enabled = canNavigateBack,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Localized description",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        NavigationSuiteScaffold(
            navigationItems = {
                navigationItems.forEachIndexed { index, navigationItem ->
                    NavigationSuiteItem(
                        selected = currentTab == index,
                        onClick = {
                            applicationState.navigate(navigationItem.destination)
                        },
                        icon = {
                            Icon(navigationItem.icon, navigationItem.title)
                        },
                        label = { Text(navigationItem.title) },
                    )
                }
            },
            navigationSuiteType = navLayoutType,
            navigationItemVerticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            modifier =
                Modifier.padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                ),
            primaryActionContent = {
                if (navLayoutType == NavigationSuiteType.ShortNavigationBarCompact ||
                    navLayoutType == NavigationSuiteType.ShortNavigationBarMedium
                ) {
                    applicationState.ActionButton()
                }
            },
        ) {
            val entryProvider = entryProvider(builder = entries)
            val decorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                    remember { CustomNavEntryDecorator(applicationState) },
                )
            val decoratedEntries =
                rememberDecoratedNavEntries(
                    backStack = applicationState.state.backStack,
                    entryDecorators = decorators,
                    entryProvider = entryProvider,
                )
            NavDisplay(
                entries = decoratedEntries.toMutableStateList(),
                onBack = { applicationState.goBack() },
                sceneStrategies = remember { listOf(DialogSceneStrategy()) },
                predictivePopTransitionSpec = {
                    ContentTransform(
                        fadeIn(),
                        fadeOut(),
                    )
                },
            )
        }
    }
}
