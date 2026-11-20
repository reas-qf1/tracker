package com.reas.tracker2.api.v1.routes

import com.reas.tracker2.api.helpers.OneOrMoreSerializer
import com.reas.tracker2.api.v1.primitives.EventV1
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

object SubmitRequestV1Serializer : KSerializer<SubmitRequestV1> {
    private val serializer = OneOrMoreSerializer(Json.serializersModule.serializer<EventV1>())
    override val descriptor: SerialDescriptor = SerialDescriptor(javaClass.name, serializer.descriptor)

    override fun serialize(encoder: Encoder, value: SubmitRequestV1) = serializer.serialize(encoder, value.events)

    override fun deserialize(decoder: Decoder) = SubmitRequestV1(serializer.deserialize(decoder))
}

@Serializable(with = SubmitRequestV1Serializer::class)
data class SubmitRequestV1(
    @Serializable(with = OneOrMoreSerializer::class)
    val events: List<EventV1>,
)
