package com.naze.files.ui.viewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.abs

/**
 * One gesture recognizer shared by [ImageViewerScreen] and
 * [VideoPlayerScreen] so swipe-down-to-dismiss behaves identically in both:
 *
 * - 2+ fingers down -> always routed to [onTransform] (pinch zoom + pan).
 * - 1 finger, [isZoomed] true (a photo the user has zoomed into) -> also
 *   [onTransform], as a plain pan, so panning a zoomed photo can never be
 *   mistaken for a dismiss.
 * - 1 finger, not zoomed: nothing is decided until the drag clears the
 *   platform's own touch slop ([PointerInputScope.viewConfiguration], so it
 *   scales with device density like every other gesture in the app, not a
 *   hardcoded pixel count). Once it does, the *dominant* axis wins:
 *     - vertical and downward -> committed as a dismiss: [onDismissDrag]
 *       fires every frame with the incremental delta, [onDismissRelease]
 *       fires once on lift-off with the fling velocity (px/s) so the caller
 *       decides whether the drag crossed its own threshold.
 *     - anything else (upward, or horizontal-dominant, e.g. dragging the
 *       video scrubber) -> never consumed, so every event - past and
 *       future, in this same gesture - still reaches whatever sits
 *       underneath, letting ExoPlayer's own tap-to-toggle-controls and
 *       scrubber keep working when this sits on an overlay above
 *       [VideoPlayerScreen]'s PlayerView.
 * - If a second finger comes down mid dismiss-drag, the drag is abandoned
 *   via [onDismissCancelled] and control passes to [onTransform] instead.
 * - A gesture whose drag never cleared the slop in the first place - a
 *   tap, or the couple of px of jitter a real finger always has - is
 *   reported through [onTap].
 *
 * Nothing here touches storage, decodes anything, or does other frame-
 * costly work - it only ever forwards deltas/velocity for the caller to
 * apply as a transform (translation/scale/alpha).
 */
suspend fun PointerInputScope.detectSwipeToDismissGesture(
    isZoomed: () -> Boolean,
    onTransform: (zoomChange: Float, panChange: Offset) -> Unit,
    onDismissDrag: suspend (deltaY: Float) -> Unit,
    onDismissRelease: suspend (velocityY: Float) -> Unit,
    onDismissCancelled: suspend () -> Unit,
    onTap: () -> Unit,
) {
    val slop = viewConfiguration.touchSlop

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val velocityTracker = VelocityTracker()
        velocityTracker.addPosition(down.uptimeMillis, down.position)

        var isDismissing = false
        var directionSettled = false // true once a 1-finger drag has cleared slop and been classified
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
                        // else: horizontal-dominant or upward - genuinely a
                        // different gesture (e.g. scrubbing), not a dismiss
                        // and not a tap either. Deliberately never consumed
                        // from here on, in either branch above or below.
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
            // Never cleared the slop in any direction - a genuine tap.
            onTap()
        }
    }
}
