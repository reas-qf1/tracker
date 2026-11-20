package com.reas.tracker2

import com.reas.tracker2.database.AppDatabase
import com.reas.tracker2.database.AppRepository
import com.reas.tracker2.internalsServer.EventProcessor
import com.reas.tracker2.internalsServer.HolePlugger
import com.reas.tracker2.internalsServer.LocalServer
import com.reas.tracker2.internalsServer.Repository
import com.reas.tracker2.network.KtorNetworkRepository
import com.reas.tracker2.network.NetworkRepository
import com.reas.tracker2.network.httpClient
import com.reas.tracker2.network.landscapistInstance
import com.reas.tracker2.settings.DataStoreSettings
import com.reas.tracker2.settings.Settings
import com.reas.tracker2.settings.createDataStore
import com.reas.tracker2.ui.dialogs.delete.DeleteDialogViewModel
import com.reas.tracker2.ui.screens.albuminfo.AlbumInfoScreenViewModel
import com.reas.tracker2.ui.screens.artistinfo.ArtistInfoScreenViewModel
import com.reas.tracker2.ui.screens.charts.ChartsScreenViewModel
import com.reas.tracker2.ui.screens.debug.DebugScreenViewModel
import com.reas.tracker2.ui.screens.history.HistoryScreenViewModel
import com.reas.tracker2.ui.screens.settings.SettingsScreenViewModel
import com.reas.tracker2.ui.screens.trackhistory.TrackHistoryViewModel
import com.reas.tracker2.ui.screens.trackinfo.TrackInfoScreenViewModel
import com.reas.tracker2.ui.sheets.edit.EditBottomSheetViewModel
import com.reas.tracker2.ui.sheets.info.InfoBottomSheetsViewModel
import com.reas.tracker2.util.InMemoryLog
import com.reas.tracker2.util.SecretManager
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val sharedModule = module {
    singleOf(::createDataStore)
    singleOf(::DataStoreSettings) bind Settings::class
    single { AppRepository(AppDatabase.getDatabase(get())) } bind Repository::class
    singleOf(::KtorNetworkRepository) bind NetworkRepository::class
    singleOf(::httpClient)
    singleOf(::landscapistInstance)

    singleOf(::EventProcessor)
    singleOf(::HolePlugger)
    singleOf(::LocalServer)
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
    viewModelOf(::EditBottomSheetViewModel)
    viewModelOf(::DeleteDialogViewModel)
}
