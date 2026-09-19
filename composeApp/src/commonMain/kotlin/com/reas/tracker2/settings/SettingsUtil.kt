package com.reas.tracker2.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import com.reas.tracker2.util.PlatformDependentPaths
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import kotlin.enums.enumEntries

interface Setting<K, T> {
    val preferencesKey: Preferences.Key<K>
    val defaultValue: T

    fun save(value: T): K

    fun restore(value: K): T

    companion object {
        fun int(key: String, default: Int) = IntSetting(key = key, default = default)

        fun string(key: String, default: String) = StringSetting(key = key, default = default)

        fun boolean(key: String, default: Boolean) = BooleanSetting(key = key, default = default)

        inline fun <reified T : Enum<T>> enum(key: String, default: T) =
            object : Setting<Int, T> {
                override val preferencesKey: Preferences.Key<Int> = intPreferencesKey(key)
                override val defaultValue: T = default

                override fun save(value: T): Int = value.ordinal

                override fun restore(value: Int): T = enumEntries<T>()[value]
            }
    }
}

interface SimpleSetting<T> : Setting<T, T> {
    override fun save(value: T): T = value

    override fun restore(value: T): T = value
}

class IntSetting(
    key: String,
    default: Int,
) : SimpleSetting<Int> {
    override val preferencesKey: Preferences.Key<Int> = intPreferencesKey(key)
    override val defaultValue: Int = default
}

class BooleanSetting(
    key: String,
    default: Boolean,
) : SimpleSetting<Boolean> {
    override val preferencesKey: Preferences.Key<Boolean> = booleanPreferencesKey(key)
    override val defaultValue: Boolean = default
}

class StringSetting(
    key: String,
    default: String,
) : SimpleSetting<String> {
    override val preferencesKey: Preferences.Key<String> = stringPreferencesKey(key)
    override val defaultValue: String = default
}

interface SettingsEditScope {
    infix fun <K, T> Setting<K, T>.to(value: T)
}

interface Settings {
    fun <K, T> flow(setting: Setting<K, T>): Flow<T>

    suspend fun <K, T> collect(setting: Setting<K, T>, collector: suspend (T) -> Unit)

    suspend operator fun <K, T> get(setting: Setting<K, T>): T

    fun <K, T> getBlocking(setting: Setting<K, T>): T

    suspend operator fun <K, T> set(setting: Setting<K, T>, value: T)

    suspend fun edit(block: SettingsEditScope.() -> Unit)
}

class DataStoreSettingsEditScope(
    private val preferences: MutablePreferences,
) : SettingsEditScope {
    override fun <K, T> Setting<K, T>.to(value: T) {
        preferences[this.preferencesKey] = this.save(value)
    }
}

class DataStoreSettings(
    private val dataStore: DataStore<Preferences>,
) : Settings {
    override fun <K, T> flow(setting: Setting<K, T>) =
        dataStore.data.map { preferences ->
            val value = preferences[setting.preferencesKey]
            value?.let { setting.restore(it) } ?: setting.defaultValue
        }

    override suspend fun <K, T> collect(setting: Setting<K, T>, collector: suspend (T) -> Unit) {
        flow(setting).drop(1).collect(collector)
    }

    override suspend fun <K, T> get(setting: Setting<K, T>): T = flow(setting).first()

    override fun <K, T> getBlocking(setting: Setting<K, T>): T = runBlocking { get(setting) }

    override suspend fun <K, T> set(setting: Setting<K, T>, value: T) {
        dataStore.edit { preferences ->
            preferences[setting.preferencesKey] = setting.save(value)
        }
    }

    override suspend fun edit(block: SettingsEditScope.() -> Unit) {
        dataStore.edit { preferences ->
            DataStoreSettingsEditScope(preferences).block()
        }
    }
}

fun createDataStore(pathProvider: PlatformDependentPaths): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { pathProvider.getPreferencesPath().toPath() },
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    )

internal const val dataStoreFileName = "tracker2.preferences_pb"
