package com.reas.tracker2

import com.reas.tracker2.api.v1.ApiV1Impl
import com.reas.tracker2.database.AuthRepository
import com.reas.tracker2.database.DatabaseAuthRepository
import com.reas.tracker2.database.createInMemoryDatabase
import com.reas.tracker2.shared.EventProcessor
import com.reas.tracker2.shared.HolePlugger
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val module = module {
    singleOf(::createInMemoryDatabase)
    singleOf(::DatabaseAuthRepository) bind AuthRepository::class

    singleOf(::ApiV1Impl)

    singleOf(::EventProcessor)
    single { HolePlugger() }
}
