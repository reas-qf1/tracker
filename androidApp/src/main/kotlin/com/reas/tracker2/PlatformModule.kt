package com.reas.tracker2

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import com.reas.tracker2.android.MediaEventRelay
import com.reas.tracker2.android.NotificationWrapper
import com.reas.tracker2.database.getDatabaseBuilder
import com.reas.tracker2.util.NowPlayingNotificationManager
import com.reas.tracker2.util.NowPlayingNotificationManagerAndroid
import com.reas.tracker2.util.PlatformDependentPaths
import com.reas.tracker2.util.PlatformDependentPathsAndroid
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val platformModule =
    module {
        singleOf(::PlatformDependentPathsAndroid) bind PlatformDependentPaths::class
        singleOf(::NowPlayingNotificationManagerAndroid) bind NowPlayingNotificationManager::class
        singleOf(::getDatabaseBuilder)
        single {
            NotificationWrapper(
                get(),
                get<Context>().getSystemService(NOTIFICATION_SERVICE) as NotificationManager,
            )
        }
        singleOf(::MediaEventRelay)
    }
