package com.reas.tracker2.network

import com.reas.tracker2.shared.Album
import com.reas.tracker2.util.SecretManager
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface NetworkRepository {
    suspend fun getAlbumImageUrl(album: Album, size: String): String?
}

class KtorNetworkRepository :
    NetworkRepository,
    KoinComponent {
    private val client: HttpClient by inject()
    private val secrets: SecretManager by inject()

    override suspend fun getAlbumImageUrl(album: Album, size: String): String? {
        val key = secrets.lastfmApiKey
        if (key == null) {
            logger.warn { "couldn't fetch image from lastfm: no api key provided" }
            return null
        }
        val response =
            client.get {
                url {
                    host = "ws.audioscrobbler.com"
                    path("2.0")
                    parameters.apply {
                        append("method", "album.getinfo")
                        append("api_key", key)
                        append("format", "json")
                        append("artist", album.artistsAsString)
                        append("album", album.name)
                    }
                }
            }
        return response
            .body<LastFMAlbumInfoWrapper>()
            .album.image
            .firstOrNull { it.size == size }
            ?.url
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
