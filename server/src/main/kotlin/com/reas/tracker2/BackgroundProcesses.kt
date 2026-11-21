package com.reas.tracker2

import com.reas.tracker2.internalsServer.LocalServer
import io.ktor.server.application.*
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject

fun Application.backgroundProcesses() {
    val server: LocalServer by inject()

    launch {
        server.startup()
    }
}
