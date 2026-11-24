package com.reas.tracker2.api.v1.primitives

import com.reas.tracker2.shared.ArtistList
import kotlinx.serialization.Serializable

@Serializable
data class ArtistListV1(
    val string: String,
    val list: List<IDObjectV1>,
) {
    companion object {
        fun from(artistList: ArtistList): ArtistListV1 =
            ArtistListV1(
                string = artistList.raw,
                list = artistList.artists.map {
                    IDObjectV1(
                        id = it.id,
                        name = it.name,
                    )
                },
            )
    }
}
