package com.reas.tracker2.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LastFMAlbumInfoWrapper(
    val album: LastFMAlbumInfo,
)

@Serializable
data class LastFMAlbumInfo(
    val image: List<LastFMImageInfo>,
)

@Serializable
data class LastFMImageInfo(
    val size: String,
    @SerialName("#text") val url: String,
)
