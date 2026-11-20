package com.reas.tracker2

import com.reas.tracker2.api.v1.ApiV1Impl
import com.reas.tracker2.database.AuthRepository
import com.reas.tracker2.database.DataRepository
import com.reas.tracker2.database.DatabaseAuthRepository
import com.reas.tracker2.database.createSQLiteDatabase
import com.reas.tracker2.internalsServer.EventProcessor
import com.reas.tracker2.internalsServer.HolePlugger
import com.reas.tracker2.internalsServer.LocalServer
import com.reas.tracker2.internalsServer.Repository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val module = module {
    singleOf(::createSQLiteDatabase)
    singleOf(::DatabaseAuthRepository) bind AuthRepository::class
    singleOf(::DataRepository) bind Repository::class

    singleOf(::EventProcessor)
    singleOf(::HolePlugger)
    singleOf(::LocalServer)

    singleOf(::ApiV1Impl)
}
