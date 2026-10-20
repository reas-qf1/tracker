package com.reas.tracker2.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetAt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

fun Duration.toDisplayString() =
    when {
        this.isNegative() -> "..."
        this < 1.minutes -> inWholeSeconds.seconds.toString()
        else -> inWholeMinutes.minutes.toString()
    }

fun Instant.printShort() =
    this.format(
        DateTimeComponents.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            day()
            char(',')
            char(' ')
            year()
        },
        offset = TimeZone.currentSystemDefault().offsetAt(this),
    )

fun Instant.printLong() =
    this.format(
        DateTimeComponents.Format {
            monthName(MonthNames.ENGLISH_FULL)
            char(' ')
            day()
            char(',')
            char(' ')
            year()
            char(' ')
            hour()
            char(':')
            minute()
            char(':')
            second()
        },
        offset = TimeZone.currentSystemDefault().offsetAt(this),
    )
