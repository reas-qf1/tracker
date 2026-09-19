package com.reas.tracker2.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Duration.toDisplayString() =
    when {
        this.isNegative() -> "..."
        this < 1.minutes -> inWholeSeconds.seconds.toString()
        else -> inWholeMinutes.minutes.toString()
    }
