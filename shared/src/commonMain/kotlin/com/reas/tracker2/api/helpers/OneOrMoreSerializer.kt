package com.reas.tracker2.api.helpers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer

class OneOrMoreSerializer<T>(
    serializer: KSerializer<T>,
) : JsonTransformingSerializer<List<T>>(ListSerializer(serializer)) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        // If the element is not a JsonArray, wraps it into a single-element array
        return element as? JsonArray ?: JsonArray(listOf(element))
    }

    override fun transformSerialize(element: JsonElement): JsonElement {
        require(element is JsonArray)
        // Unwraps single-element lists into a single JSON object
        return element.singleOrNull() ?: element
    }
}
