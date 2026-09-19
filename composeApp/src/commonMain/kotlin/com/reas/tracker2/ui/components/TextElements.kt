package com.reas.tracker2.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.reas.tracker2.ui.derivedState
import com.reas.tracker2.ui.state
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetAt
import org.jetbrains.compose.resources.stringResource
import tracker2.composeapp.generated.resources.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private fun Modifier.firstBaselineToTop(firstBaselineToTop: Dp) =
    layout { measurable, constraints ->
        // Measure the composable
        val placeable = measurable.measure(constraints)

        // Check the composable has a first baseline
        check(placeable[FirstBaseline] != AlignmentLine.Unspecified)
        val firstBaseline = placeable[FirstBaseline]

        // Height of the composable with padding - first baseline
        val placeableY = firstBaselineToTop.roundToPx() - firstBaseline
        layout(placeable.width, placeable.height) {
            // Where the composable gets placed
            placeable.placeRelative(0, placeableY)
        }
    }

@Composable
fun ConstrainedText(
    text: String,
    height: Dp,
    baselineHeight: Dp,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    Box(
        modifier = modifier.height(height).wrapContentHeight(align = Alignment.Top, unbounded = true),
    ) {
        Text(
            text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.firstBaselineToTop(baselineHeight),
        )
    }
}

@Composable
fun AutosizingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    Text(
        text,
        style = style,
        color = color,
        autoSize = TextAutoSize.StepBased(maxFontSize = style.fontSize),
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
fun Timestamp(
    timestamp: Instant,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.secondary,
            ) {
                Text(timestamp.printLong())
            }
        },
        state = tooltipState,
    ) {
        var difference by state(Clock.System.now() - timestamp)
        val showSeconds by derivedState {
            difference < 1.minutes
        }
        val showMinutes by derivedState {
            1.minutes <= difference && difference < 1.hours
        }
        val showHours by derivedState {
            1.hours <= difference && difference < 1.days
        }
        val showDays by derivedState {
            1.days <= difference && difference < 30.days
        }
        val showDate by derivedState {
            difference >= 30.days
        }

        val text =
            if (showSeconds) {
                stringResource(Res.string.seconds_ago, difference.inWholeSeconds)
            } else if (showMinutes) {
                stringResource(Res.string.minutes_ago, difference.inWholeMinutes)
            } else if (showHours) {
                stringResource(Res.string.hours_ago, difference.inWholeHours)
            } else if (showDays) {
                stringResource(Res.string.days_ago, difference.inWholeDays)
            } else {
                timestamp.printShort()
            }

        Text(
            text,
            modifier = modifier.clickable(onClick = {
                scope.launch { tooltipState.show() }
            }),
            style = style,
            color = color,
        )

        if (!showDate) {
            LaunchedEffect(difference) {
                if (showSeconds) {
                    delay((difference.inWholeSeconds + 1).seconds - difference)
                }
                if (showMinutes) {
                    delay((difference.inWholeMinutes + 1).minutes - difference)
                }
                if (showHours) {
                    delay((difference.inWholeHours + 1).hours - difference)
                }
                if (showDays) {
                    delay((difference.inWholeDays + 1).days - difference)
                }
                difference = Clock.System.now() - timestamp
            }
        }
    }
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
