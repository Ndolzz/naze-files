package com.naze.files.ui.viewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.abs

/**
 * One gesture recognizer shared by [ImageViewerScreen] and
 * [VideoPlayerScreen] so swipe-down-to-dismiss behaves identically in both.
 */
suspend fun PointerInputScope.detectSwipeToDismissGesture(
    isZoomed: () -> Boolean,
    onTransform: (zoomChange: Float, panChange: Offset) -> Unit,
    onDismissDrag: (deltaY: Float) -> Unit,
    onDismissRelease: (velocityY: Float) -> Unit,
    onDismissCancelled: () -> Unit,
    onTap: () -> Unit,
) {
    val slop = viewConfiguration.touchSlop

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val velocityTracker = VelocityTracker()
        velocityTracker.addPosition(down.uptimeMillis, down.position)

        var isDismissing = false
        var directionSettled = false
        var pendingX = 0f
        var pendingY = 0f

        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }

            if (pressed.size >= 2) {
                if (isDismissing) {
                    isDismissing = false
                    onDismissCancelled()
                }
                directionSettled = true
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                if (zoomChange != 1f || panChange != Offset.Zero) {
                    onTransform(zoomChange, panChange)
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            } else if (pressed.size == 1) {
                val change = pressed[0]
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                val drag = change.positionChange()

                if (isZoomed()) {
                    if (drag != Offset.Zero) {
                        onTransform(1f, drag)
                        change.consume()
                    }
                } else if (!directionSettled) {
                    pendingX += drag.x
                    pendingY += drag.y
                    if (abs(pendingX) > slop || abs(pendingY) > slop) {
                        directionSettled = true
                        isDismissing = abs(pendingY) > abs(pendingX) && pendingY > 0f
                        if (isDismissing) {
                            onDismissDrag(pendingY)
                            change.consume()
                        }
                    }
                } else if (isDismissing) {
                    onDismissDrag(drag.y)
                    change.consume()
                }
            }

            if (event.changes.none { it.pressed }) break
        }

        if (isDismissing) {
            onDismissRelease(velocityTracker.calculateVelocity().y)
        } else if (!directionSettled) {
            onTap()
        }
    }
}
