package com.reas.tracker2.api.v1.primitives

import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

class ArtistListV1Serializer : JsonTransformingSerializer<ArtistListV1>(ArtistListV1.generatedSerializer()) {
    override fun transformSerialize(element: JsonElement): JsonElement =
        element as? JsonObject ?: buildJsonObject {
            put("artists", element as JsonArray)
            put("raw", element.jsonArray.joinToString(", ") { (it as JsonPrimitive).content })
        }
}

@KeepGeneratedSerializer
@Serializable(with = ArtistListV1Serializer::class)
data class ArtistListV1(
    val list: List<String>,
    val raw: String,
)
