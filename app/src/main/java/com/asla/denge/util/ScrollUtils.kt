package com.asla.denge.util

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.withFrameNanos

/**
 * Smoothly scrolls a LazyListState directly to the top in a single continuous animation.
 * Replicates a fluid manual flick/fling with dynamic deceleration towards item 0,
 * eliminating the 2-phase pause/stutter that happens at large cards like Now Playing.
 */
suspend fun LazyListState.smoothScrollToTop() {
    if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) return

    scroll(MutatePriority.Default) {
        var lastFrameTime = withFrameNanos { it }
        var currentSpeed = 0f // px per millisecond
        val maxSpeed = 10f // ~10,000 px/sec max speed, very fluid
        val minSpeed = 1.2f

        while (firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0) {
            val frameTime = withFrameNanos { it }
            val dtMs = ((frameTime - lastFrameTime) / 1_000_000L).coerceIn(1, 32)
            lastFrameTime = frameTime

            // Dynamic speed smoothly scaled to how close we are to the top
            val targetSpeed = when {
                firstVisibleItemIndex > 6 -> maxSpeed
                firstVisibleItemIndex > 3 -> 6.5f
                firstVisibleItemIndex > 1 -> 3.5f
                firstVisibleItemIndex == 1 -> 2.2f
                firstVisibleItemScrollOffset > 200 -> 1.8f
                else -> minSpeed
            }

            // Smooth interpolation to avoid abrupt velocity jumps
            currentSpeed += (targetSpeed - currentSpeed) * 0.28f

            val step = currentSpeed * dtMs
            val consumed = scrollBy(-step)

            // When reached top and no more offset can be consumed
            if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) {
                break
            }
            if (consumed == 0f && currentSpeed > 0f) {
                break
            }
        }
    }
}
