package com.reas.tracker2.network

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

private object KtorClient {
    val logger = KotlinLogging.logger {}
}

fun httpClient() =
    HttpClient(CIO) {
        install(HttpCache)
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    KtorClient.logger.trace { message }
                }
            }
        }
        install(WebSockets) {
            pingInterval = 15.seconds
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }
