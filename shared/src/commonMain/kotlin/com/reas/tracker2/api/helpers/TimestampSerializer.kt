package com.reas.tracker2.api.helpers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.time.Instant

class TimestampSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("timestamp", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        require(encoder is JsonEncoder)
        val element = encoder.json.encodeToJsonElement(Instant.serializer(), value)
        encoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): Instant {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement() as JsonPrimitive
        if (element.isString) return Instant.parse(element.content)
        val value = element.doubleOrNull?.toLong() ?: element.long
        return if (value <= 100_000_000_000) {
            Instant.fromEpochSeconds(value)
        } else {
            Instant.fromEpochMilliseconds(value)
        }
    }
}
