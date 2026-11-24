package com.reas.tracker2.api.v1.primitives

import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

class RawArtistListV1Serializer : JsonTransformingSerializer<RawArtistListV1>(RawArtistListV1.generatedSerializer()) {
    override fun transformSerialize(element: JsonElement): JsonElement =
        element as? JsonObject ?: buildJsonObject {
            put("artists", element as JsonArray)
            put("raw", element.jsonArray.joinToString(", ") { (it as JsonPrimitive).content })
        }
}

@KeepGeneratedSerializer
@Serializable(with = RawArtistListV1Serializer::class)
data class RawArtistListV1(
    val list: List<String>,
    val raw: String,
)
