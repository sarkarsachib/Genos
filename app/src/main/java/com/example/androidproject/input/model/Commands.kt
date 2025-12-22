package com.example.androidproject.input.model

import androidx.annotation.IntRange

/**
 * Sealed class representing all possible input commands that can be executed by the InputExecutor.
 */
sealed class InputCommand {
    /**
     * Represents a tap gesture at one or more points.
     * 
     * @property points List of tap coordinates
     * @property durationMs Duration of the tap gesture (defaults to 100ms for a quick tap)
     */
    data class TapCommand(
        val points: List<Point>,
        @IntRange(from = 1) val durationMs: Long = DEFAULT_TAP_DURATION_MS
    ) : InputCommand() {
        constructor(x: Int, y: Int, durationMs: Long = DEFAULT_TAP_DURATION_MS) : 
            this(listOf(Point(x, y)), durationMs)
    }

    /**
     * Represents a swipe gesture from start to end point.
     * 
     * @property startX Starting X coordinate
     * @property startY Starting Y coordinate
     * @property endX Ending X coordinate
     * @property endY Ending Y coordinate
     * @property durationMs Duration of the swipe (microseconds precision)
     */
    data class SwipeCommand(
        val startX: Int,
        val startY: Int,
        val endX: Int,
        val endY: Int,
        @IntRange(from = 1) val durationMs: Long = DEFAULT_SWIPE_DURATION_MS
    ) : InputCommand()

    /**
     * Represents a scroll gesture with delta values.
     * 
     * @property deltaX Horizontal scroll amount (positive = scroll right)
     * @property deltaY Vertical scroll amount (positive = scroll down)
     * @property x Start X coordinate
     * @property y Start Y coordinate
     * @property scrollDurationMs Duration of the scroll gesture
     */
    data class ScrollCommand(
        val deltaX: Int,
        val deltaY: Int,
        val x: Int = DEFAULT_SCROLL_X,
        val y: Int = DEFAULT_SCROLL_Y,
        @IntRange(from = 1) val scrollDurationMs: Long = DEFAULT_SCROLL_DURATION_MS
    ) : InputCommand()

    /**
     * Represents text input into the currently focused field.
     * 
     * @property text Text to type
     * @property clearExisting Whether to clear existing text before typing
     * @property commitIme Whether to commit IME text (ENTER key simulation)
     */
    data class TypeCommand(
        val text: String,
        val clearExisting: Boolean = false,
        val commitIme: Boolean = false
    ) : InputCommand()

    companion object {
        private const val DEFAULT_TAP_DURATION_MS = 100L
        private const val DEFAULT_SWIPE_DURATION_MS = 300L
        private const val DEFAULT_SCROLL_DURATION_MS = 200L
        private const val DEFAULT_SCROLL_X = 500
        private const val DEFAULT_SCROLL_Y = 800
    }
}

/**
 * Represents a coordinate point on the screen.
 */
data class Point(
    val x: Int,
    val y: Int
) {
    /**
     * Checks whether this point lies within the inclusive bounds from (0, 0) to (maxX, maxY).
     *
     * @param maxX The maximum allowed X coordinate (inclusive).
     * @param maxY The maximum allowed Y coordinate (inclusive).
     * @return `true` if `0 <= x <= maxX` and `0 <= y <= maxY`, `false` otherwise.
     */
    fun isValid(maxX: Int, maxY: Int): Boolean {
        return x >= 0 && x <= maxX && y >= 0 && y <= maxY
    }
}