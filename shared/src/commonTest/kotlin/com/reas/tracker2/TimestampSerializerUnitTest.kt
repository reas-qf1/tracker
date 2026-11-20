package com.reas.tracker2

import com.reas.tracker2.api.helpers.TimestampSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class TimestampSerializerUnitTest {
    @Test
    fun testTimestampSerializer() {
        val timestamp = Instant.fromEpochMilliseconds(1000000000001)
        assertEquals("\"2001-09-09T01:46:40.001Z\"", Json.encodeToString(TimestampSerializer(), timestamp))
    }

    @Test
    fun testTimestampDeserializerString() {
        val input = "\"2001-09-09T01:46:40.000Z\""
        assertEquals(1000000000, Json.decodeFromString(TimestampSerializer(), input).epochSeconds)
    }

    @Test
    fun testTimestampDeserializerIntegerInSeconds() {
        val input = "1000000000"
        assertEquals(1000000000, Json.decodeFromString(TimestampSerializer(), input).toEpochMilliseconds())
    }

    @Test
    fun testTimestampDeserializerIntegerInMilliseconds() {
        val input = "1000000000000"
        assertEquals(1000000000, Json.decodeFromString(TimestampSerializer(), input).epochSeconds)
    }
}
