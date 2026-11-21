package com.reas.tracker2.database

import com.reas.tracker2.shared.EventInfo
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ColumnTransformer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class InstantTransformer : ColumnTransformer<Long, Instant> {
    override fun unwrap(value: Instant): Long = value.toEpochMilliseconds()

    override fun wrap(value: Long): Instant = Instant.fromEpochMilliseconds(value)
}

class DurationTransformer : ColumnTransformer<Long, Duration> {
    override fun unwrap(value: Duration): Long = value.inWholeMilliseconds

    override fun wrap(value: Long): Duration = value.milliseconds
}

class EventInfoListTransformer : ColumnTransformer<String, List<EventInfo>> {
    override fun unwrap(value: List<EventInfo>): String = Json.encodeToString(value)

    override fun wrap(value: String): List<EventInfo> = Json.decodeFromString(value)
}
