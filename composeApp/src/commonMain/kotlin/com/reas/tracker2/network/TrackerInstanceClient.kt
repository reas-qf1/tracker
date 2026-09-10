 package com.reas.tracker2.network

import com.reas.tracker2.api.EventAPI
import com.reas.tracker2.api.PlayAPI
import com.reas.tracker2.database.Repository
import com.reas.tracker2.settings.Settings
import com.reas.tracker2.settings.instanceHostName
import com.reas.tracker2.settings.instancePort
import com.reas.tracker2.settings.username
import com.reas.tracker2.shared.Event
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.path
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

 data class SyncEvent(
    val id: Long,
    val event: Event,
)

class TrackerInstanceClient(
    private val repository: Repository,
    private val client: HttpClient,
    private val settings: Settings
) {
    private val host_
        get() = settings.getBlocking(instanceHostName)
    private val port_
        get() = settings.getBlocking(instancePort)
    private val username_
        get() = settings.getBlocking(username)
    private var apiKey = ""

    private val queueLock = Mutex()

    suspend fun tryLogin(): String? {
        repository.getKey(host_, port_, username_)?.let {
            apiKey = it
            return null
        }
        try {
            val r = client.post {
                url { request ->
                    request.host = host_
                    request.port = port_
                    path("login")
                    parameters.append("user", username_)
                }
            }
            if (!r.status.isSuccess()) {
                logger.warn { "Error while trying to login: ${r.status}" }
                return r.status.description
            }
            apiKey = r.bodyAsText()
            repository.addKey(host_, port_, username_, apiKey)
            return null
        } catch (e: Exception) {
            logger.warn(throwable = e) { "Error while trying to login" }
            return "${e.javaClass.name}${if (e.message != null) ": " + e.message else ""}"
        }
    }

    suspend fun tryEstablishConnection(): Boolean {
        try {
            logger.debug { "connecting to server" }
            client.webSocket(
                request = {
                    url {
                        host = host_
                        port = port_
                        url("sync")
                        parameters.append("api_key", apiKey)
                    }
                }
            ) {
                logger.debug { "connected to server" }

                // TODO: figure out something better
                launch { submitEvents() }

                val receiveJob = launch {
                    incoming.consumeEach { frame ->
                        if (frame !is Frame.Text) return@consumeEach
                        val body = frame.readText()
                        val plays = try {
                            listOf(Json.decodeFromString<PlayAPI>(body))
                        } catch (e: SerializationException) {
                            Json.decodeFromString<List<PlayAPI>>(body)
                        } catch (e: SerializationException) {
                            logger.warn(throwable = e) { "couldn't deserialize plays from server" }
                            return@consumeEach
                        }

                        repository.insertPlays(plays.filter {
                            it.client != apiKey
                        }.map { it.toPlay() })
                    }
                }

                outgoing.invokeOnClose {
                    logger.debug { "connection closed" }
                    receiveJob.cancel()
                }

                receiveJob.join()
            }
            return true
        } catch (e: Exception) {
            logger.warn(throwable = e) { "connection error" }
            return false
        }
    }

    suspend fun submitEvent(event: Event) {
        // TODO: figure out something better
        repository.insertEventInSync(event)
        submitEvents()
    }

    suspend fun submitEvents() {
        if (host_ == "" || port_ == 0 || username_ == "")
            return

        // apiKey here can be empty if app hasn't been opened since device reboot
        if (apiKey == "") {
            tryLogin()
        }

        queueLock.withLock {
            val events = repository.getEventsInSync().first()
            if (events.isEmpty()) return
            events.chunked(100).forEach { batch ->
                logger.debug { "submitting ${batch.size} events from queue" }
                try {
                    val r = client.post {
                        url {
                            host = host_
                            port = port_
                            path("scrobble")
                            parameters.append("api_key", apiKey)
                        }
                        contentType(ContentType.Application.Json)
                        setBody(batch.map { EventAPI.fromEvent(it.event) })
                    }
                    if (!r.status.isSuccess()) {
                        logger.debug { "event submit failed with code ${r.status.value}" }
                        return
                    } else {
                        logger.debug { "event submit successful" }
                        repository.deleteFromSync(batch.map { it.id })
                    }
                } catch (e: Exception) {
                    logger.warn(throwable = e) { "event submit failed" }
                    return
                }
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}