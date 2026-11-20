package com.reas.tracker2.api.v1.routes

import com.reas.tracker2.api.v1.primitives.ScrobbleV1
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

object SubmitResponseV1Serializer : KSerializer<SubmitResponseV1> {
    private val serializer = Json.serializersModule.serializer<List<ScrobbleV1>>()
    override val descriptor: SerialDescriptor = SerialDescriptor(javaClass.name, serializer.descriptor)

    override fun serialize(encoder: Encoder, value: SubmitResponseV1) = serializer.serialize(encoder, value.scrobbles)

    override fun deserialize(decoder: Decoder) = SubmitResponseV1(serializer.deserialize(decoder))
}

@Serializable(with = SubmitResponseV1Serializer::class)
data class SubmitResponseV1(
    val scrobbles: List<ScrobbleV1>,
)
