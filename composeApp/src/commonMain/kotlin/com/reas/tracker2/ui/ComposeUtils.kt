package com.reas.tracker2.ui

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Composable
inline fun <reified T> state(initialValue: T) =
    remember {
        when (initialValue) {
            // is this even a good idea?
            is Int -> mutableIntStateOf(initialValue) as MutableState<T>

            is Long -> mutableLongStateOf(initialValue) as MutableState<T>

            is Float -> mutableFloatStateOf(initialValue) as MutableState<T>

            is Double -> mutableDoubleStateOf(initialValue) as MutableState<T>

            else -> mutableStateOf(initialValue)
        }
    }

@Composable
inline fun <reified T> derivedState(crossinline state: () -> T) = remember { derivedStateOf { state() } }

@Composable
inline fun <reified T> rememberAsState(crossinline state: () -> StateFlow<T>) = remember { state() }.collectAsStateWithLifecycle()

@Composable
inline fun <reified T : Any> rememberAsPagingItems(crossinline state: () -> Flow<PagingData<T>>) =
    remember { state() }.collectAsLazyPagingItems()
