package com.reas.tracker2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.reas.tracker2.settings.Setting
import com.reas.tracker2.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration

open class TrackerViewModel : ViewModel() {
    internal fun <T> Flow<T>.asStateFlow(initialValue: T) =
        stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = initialValue,
        )

    internal fun Flow<String>.asStringStateFlow() = asStateFlow(initialValue = "...")

    internal fun Flow<Int>.asIntStateFlow() = asStateFlow(initialValue = -1)

    internal fun Flow<Duration>.asDurationStateFlow() = asStateFlow(initialValue = -Duration.INFINITE)

    internal fun <T> Flow<List<T>>.asListStateFlow() = asStateFlow(initialValue = listOf())

    internal fun <T, R> Flow<List<T>>.mapElements(transform: (T) -> R) = map { it.map(transform) }

    internal fun <T : Any> pagingDataFlow(pagingSourceFactory: () -> PagingSource<Int, T>) =
        Pager(
            initialKey = 0,
            pagingSourceFactory = pagingSourceFactory,
            config = PagingConfig(pageSize = PAGE_SIZE, initialLoadSize = PAGE_SIZE),
        ).flow.cachedIn(viewModelScope)

    internal fun <T : Any, R : Any> Flow<PagingData<T>>.mapElements(transform: suspend (T) -> R) = map { it.map(transform) }

    internal fun <K, T> Settings.stateFlow(setting: Setting<K, T>) = flow(setting).asStateFlow(this.getBlocking(setting))

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
        private const val PAGE_SIZE = 50
    }
}
