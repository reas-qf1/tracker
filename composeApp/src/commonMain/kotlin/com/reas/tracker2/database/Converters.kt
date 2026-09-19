package com.reas.tracker2.database

import androidx.room3.ColumnTypeConverter
import com.reas.tracker2.shared.EventInfo
import com.reas.tracker2.shared.EventState
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

class Converters {
    @ColumnTypeConverter
    fun eventInfoListToString(list: MutableList<EventInfo>): String = Json.encodeToString(list)

    @ColumnTypeConverter
    fun stringToEventInfoList(value: String): MutableList<EventInfo> = Json.decodeFromString(value)

    @ColumnTypeConverter
    fun instantToLong(instant: Instant): Long = instant.toEpochMilliseconds()

    @ColumnTypeConverter
    fun longToInstant(long: Long): Instant = Instant.fromEpochMilliseconds(long)

    @ColumnTypeConverter
    fun durationToLong(duration: Duration): Long = duration.inWholeMilliseconds

    @ColumnTypeConverter
    fun longToDuration(long: Long): Duration = long.milliseconds

    @ColumnTypeConverter
    fun idListToString(list: List<Long>): String = list.joinToString(",")

    @ColumnTypeConverter
    fun stringToIdList(string: String): List<Long> = string.split(",").map { it.toLong() }

    @ColumnTypeConverter
    fun eventStateToString(state: EventState): String = state.name

    @ColumnTypeConverter
    fun stringToEventState(value: String): EventState = EventState.valueOf(value)

    @ColumnTypeConverter
    fun uuidToString(uuid: Uuid) = uuid.toHexDashString()

    @ColumnTypeConverter
    fun stringToUuid(string: String) = Uuid.parseHexDash(string)
}
