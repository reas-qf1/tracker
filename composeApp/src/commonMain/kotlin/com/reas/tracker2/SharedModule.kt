package com.reas.tracker2

import com.reas.tracker2.database.AppDatabase
import com.reas.tracker2.database.Repository
import com.reas.tracker2.database.RoomRepository
import com.reas.tracker2.network.KtorNetworkRepository
import com.reas.tracker2.network.NetworkRepository
import com.reas.tracker2.network.httpClient
import com.reas.tracker2.network.landscapistInstance
import com.reas.tracker2.settings.DataStoreSettings
import com.reas.tracker2.settings.Settings
import com.reas.tracker2.settings.createDataStore
import com.reas.tracker2.shared.EventProcessor
import com.reas.tracker2.shared.EventProcessorAdapter
import com.reas.tracker2.shared.HolePlugger
import com.reas.tracker2.ui.viewmodels.*
import com.reas.tracker2.util.EventProcessorAdapterImpl
import com.reas.tracker2.util.InMemoryLog
import com.reas.tracker2.util.SecretManager
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val sharedModule = module {
    singleOf(::createDataStore)
    singleOf(::DataStoreSettings) bind Settings::class
    single {
        RoomRepository(AppDatabase.getDatabase(get()))
    } bind Repository::class
    singleOf(::KtorNetworkRepository) bind NetworkRepository::class
    singleOf(::httpClient)
    singleOf(::landscapistInstance)

    singleOf(::EventProcessorAdapterImpl) bind EventProcessorAdapter::class
    singleOf(::EventProcessor)
    single { HolePlugger() }
    singleOf(::InMemoryLog)
    singleOf(::SecretManager)

    viewModelOf(::HistoryScreenViewModel)
    viewModelOf(::ChartsScreenViewModel)
    viewModelOf(::InfoBottomSheetsViewModel)
    viewModelOf(::TrackHistoryViewModel)
    viewModelOf(::ArtistInfoScreenViewModel)
    viewModelOf(::AlbumInfoScreenViewModel)
    viewModelOf(::TrackInfoScreenViewModel)
    viewModelOf(::SettingsScreenViewModel)
    viewModelOf(::DebugScreenViewModel)
}
