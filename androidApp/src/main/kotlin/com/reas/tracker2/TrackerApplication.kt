package com.reas.tracker2

import android.app.Application
import android.util.Log
import io.github.oshai.kotlinlogging.*
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class TrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        KotlinLoggingConfiguration.loggerFactory =
            object : KLoggerFactory {
                override fun logger(name: String) =
                    object : KLogger {
                        override val name: String
                            get() = name

                        override fun isLoggingEnabledFor(
                            level: Level,
                            marker: Marker?,
                        ): Boolean = true

                        override fun at(
                            level: Level,
                            marker: Marker?,
                            block: KLoggingEventBuilder.() -> Unit,
                        ) {
                            val event = KLoggingEventBuilder().apply(block)
                            when (level) {
                                Level.DEBUG -> {
                                    Log.d(name, event.message, event.cause)
                                }

                                Level.INFO -> {
                                    Log.i(name, event.message, event.cause)
                                }

                                Level.WARN -> {
                                    Log.w(name, event.message, event.cause)
                                }

                                Level.ERROR -> {
                                    Log.e(name, event.message, event.cause)
                                }

                                Level.TRACE -> {
                                    Log.v(name, event.message, event.cause)
                                }

                                else -> {}
                            }
                        }
                    }
            }

        startKoin {
            androidLogger()
            androidContext(this@TrackerApplication)
            modules(platformModule, sharedModule)
        }
    }
}
