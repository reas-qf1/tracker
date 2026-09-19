package com.reas.tracker2.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import com.reas.tracker2.ui.derivedState
import com.reas.tracker2.ui.state
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.atan
import kotlin.math.roundToInt

private fun smooth(value: Float, coeff: Float) = atan(value / coeff) * coeff

@Composable
fun ScrollablePages(count: Int, modifier: Modifier = Modifier, content: @Composable (Int) -> Unit) {
    if (count == 1) {
        content(0)
    } else {
        var index by state(0)
        val offset = remember { Animatable(0f) }
        val showPrevious by derivedState { offset.value > 0 }
        val showNext by derivedState { offset.value < 0 }
        val canMoveLeft by derivedState { index > 0 }
        val canMoveRight by derivedState { index < count - 1 }
        var width by state(0)
        val scope = rememberCoroutineScope()

        Column(
            modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    width = coordinates.size.width
                }.pointerInput(Unit) {
                    // Used to calculate a settling position of a fling animation.
                    val decay = splineBasedDecay<Float>(density = this)
                    // Wrap in a coroutine scope to use suspend functions for touch events and animation.
                    coroutineScope {
                        while (true) {
                            // Wait for a touch down event.
                            val pointerId = awaitPointerEventScope { awaitFirstDown().id }
                            // Interrupt any ongoing animation.
                            offset.stop()
                            // Prepare for drag events and record velocity of a fling.
                            val velocityTracker = VelocityTracker()
                            // Wait for drag events.
                            awaitPointerEventScope {
                                horizontalDrag(pointerId) { change ->
                                    // Record the position after offset
                                    val horizontalDragOffset = offset.value + (change.position - change.previousPosition).x
                                    launch {
                                        // Overwrite the Animatable value while the element is dragged.
                                        offset.snapTo(horizontalDragOffset)
                                    }
                                    // Record the velocity of the drag.
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    // Consume the gesture event, not passed to external
                                    change.consumePositionChange()
                                }
                            }
                            // Dragging finished. Calculate the velocity of the fling.
                            val velocity = velocityTracker.calculateVelocity().x
                            // Calculate where the element eventually settles after the fling animation.
                            val targetOffsetX = decay.calculateTargetValue(offset.value, velocity)
                            // The animation should end as soon as it reaches these bounds.
                            offset.updateBounds(
                                lowerBound = -size.width.toFloat(),
                                upperBound = size.width.toFloat(),
                            )
                            launch {
                                if (targetOffsetX.absoluteValue <= size.width ||
                                    (targetOffsetX < 0 && index == count - 1) ||
                                    (targetOffsetX > 0 && index == 0)
                                ) {
                                    // Not enough velocity; Slide back to the default position.
                                    offset.animateTo(targetValue = 0f, initialVelocity = velocity)
                                } else {
                                    // Enough velocity to slide away the element to the edge.
                                    offset.animateDecay(velocity, decay)
                                    // The element was swiped away.
                                    offset.snapTo(0f)
                                    if (targetOffsetX < 0) {
                                        index++
                                    } else {
                                        index--
                                    }
                                }
                            }
                        }
                    }
                },
        ) {
            Box {
                var offset = offset.value
                if ((showPrevious && !canMoveLeft) || (showNext && !canMoveRight)) {
                    offset = smooth(offset, 60f)
                }

                repeat(count) { i ->
                    if (i == index || (i == index - 1 && showPrevious) || (i == index + 1 && showNext)) {
                        Box(Modifier.fillMaxWidth().offset { IntOffset(offset.roundToInt() + width * (i - index), 0) }) {
                            content(i)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        scope.launch {
                            offset.animateTo(width.toFloat())
                            offset.snapTo(0f)
                            index--
                        }
                    },
                    enabled = canMoveLeft,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowLeft, contentDescription = "Previous")
                }
                Spacer(Modifier.weight(1f))
                Text("${index + 1} / $count", color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = {
                        scope.launch {
                            offset.animateTo(-width.toFloat())
                            offset.snapTo(0f)
                            index++
                        }
                    },
                    enabled = canMoveRight,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = "Previous")
                }
            }
        }
    }
}
