package com.reas.tracker2.api.helpers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("duration", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Duration) {
        require(encoder is JsonEncoder)
        val element = encoder.json.encodeToJsonElement(Long.serializer(), value.inWholeMilliseconds)
        encoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): Duration {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement() as? JsonPrimitive ?: invalidInput()
        element.longOrNull?.let {
            return it.milliseconds
        }
        element.doubleOrNull?.let {
            return it.toLong().milliseconds
        }
        if (element.isString) {
            val s = element.content
            if (s.startsWith("P")) {
                return Duration.parse(s)
            }
            if (s.lowercase().endsWith("h")) {
                return s.drop(1).toDoubleOrNull()?.hours ?: invalidFormat(s)
            }
            if (s.lowercase().endsWith("m")) {
                return s.drop(1).toDoubleOrNull()?.minutes ?: invalidFormat(s)
            }
            if (s.lowercase().endsWith("s")) {
                return s.drop(1).toDoubleOrNull()?.seconds ?: invalidFormat(s)
            }
            if (s.lowercase().endsWith("ms")) {
                return s.drop(2).toDoubleOrNull()?.milliseconds ?: invalidFormat(s)
            }
            invalidFormat(s)
        }
        invalidInput()
    }

    private fun invalidInput(): Nothing = throw IllegalArgumentException("duration/position must be a string or a number")

    private fun invalidFormat(s: String): Nothing = throw IllegalArgumentException("unexpected string format for duration/position: $s")
}
