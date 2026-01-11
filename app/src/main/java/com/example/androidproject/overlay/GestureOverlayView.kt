package com.example.androidproject.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlinx.coroutines.*
import kotlin.math.atan2
import kotlin.math.hypot

class GestureOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }
    
    private val fillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 24f
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    
    private var touchPoints = mutableListOf<TouchPoint>()
    private var gesturePaths = mutableListOf<GesturePath>()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    data class TouchPoint(
        val x: Float,
        val y: Float,
        val type: TouchType,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    data class GesturePath(
        val path: Path,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val startTime: Long = System.currentTimeMillis(),
        val type: GestureType = GestureType.SWIPE
    )
    
    enum class TouchType {
        TAP,
        SWIPE_START,
        SWIPE_END,
        SCROLL
    }
    
    enum class GestureType {
        SWIPE,
        SCROLL,
        TAP
    }
    
    /**
     * Displays a temporary touch indicator at the given coordinates.
     *
     * Adds a TouchPoint of the specified type, invalidates the view to render it, and schedules
     * automatic removal of that touch indicator after 2000 milliseconds.
     *
     * @param x X coordinate in view-local pixels where the touch indicator should be shown.
     * @param y Y coordinate in view-local pixels where the touch indicator should be shown.
     * @param type The visual type of the touch indicator (e.g., TAP, SWIPE_START, SWIPE_END, SCROLL).
     */
    fun showTouch(x: Float, y: Float, type: TouchType = TouchType.TAP) {
        clearOldData()
        
        val point = TouchPoint(x, y, type)
        touchPoints.add(point)
        
        // Auto-remove after animation duration
        scope.launch {
            delay(2000)
            touchPoints.remove(point)
            invalidate()
        }
        
        invalidate()
    }
    
    /**
     * Displays a visual swipe from the given start coordinates to the end coordinates.
     *
     * Adds a swipe path and start/end touch markers, clears stale overlay data first,
     * schedules automatic removal of the gesture and its markers after 2500 ms, and
     * invalidates the view to trigger a redraw.
     *
     * @param startX X coordinate of the swipe start in view-local pixels.
     * @param startY Y coordinate of the swipe start in view-local pixels.
     * @param endX X coordinate of the swipe end in view-local pixels.
     * @param endY Y coordinate of the swipe end in view-local pixels.
     */
    fun showSwipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        clearOldData()
        
        // Create path
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        
        val gesturePath = GesturePath(path, startX, startY, endX, endY, type = GestureType.SWIPE)
        gesturePaths.add(gesturePath)
        
        // Add start and end points
        touchPoints.add(TouchPoint(startX, startY, TouchType.SWIPE_START))
        touchPoints.add(TouchPoint(endX, endY, TouchType.SWIPE_END))
        
        // Auto-remove after animation duration
        scope.launch {
            delay(2500)
            gesturePaths.remove(gesturePath)
            touchPoints.removeAll { it.timestamp == gesturePath.startTime }
            invalidate()
        }
        
        invalidate()
    }
    
    /**
     * Displays a visual scroll gesture centered at the given coordinates in the specified direction.
     *
     * Creates multiple parallel swipe paths offset around the center to represent a scroll gesture,
     * schedules the visualization to be removed after a short duration, and invalidates the view to trigger redraw.
     *
     * @param centerX X coordinate of the scroll's center.
     * @param centerY Y coordinate of the scroll's center.
     * @param direction Direction of the scroll (UP, DOWN, LEFT, RIGHT).
     * @param distance Total distance from start to end used to compute the scroll paths; defaults to 300f.
     */
    fun showScroll(centerX: Float, centerY: Float, direction: ScrollDirection, distance: Float = 300f) {
        clearOldData()
        
        val (startX, startY, endX, endY) = when (direction) {
            ScrollDirection.UP -> Triple(centerX, centerY + distance, centerX, centerY - distance)
            ScrollDirection.DOWN -> Triple(centerX, centerY - distance, centerX, centerY + distance)
            ScrollDirection.LEFT -> Triple(centerX + distance, centerY, centerX - distance, centerY)
            ScrollDirection.RIGHT -> Triple(centerX - distance, centerY, centerX + distance, centerY)
        }
        
        // Create multiple swipe paths for scroll visualization
        for (i in -2..2) {
            val offsetX = i * 20f
            val path = Path().apply {
                moveTo(startX + offsetX, startY)
                lineTo(endX + offsetX, endY)
            }
            
            val gesturePath = GesturePath(path, startX, startY, endX, endY, type = GestureType.SCROLL)
            gesturePaths.add(gesturePath)
        }
        
        // Auto-remove after animation duration
        scope.launch {
            delay(2500)
            gesturePaths.clear()
            invalidate()
        }
        
        invalidate()
    }
    
    /**
     * Removes gesture visualization data that has timed out.
     *
     * Touch points whose timestamp is older than 3000 milliseconds and gesture paths whose startTime is older than 3500 milliseconds are removed from their respective lists.
     */
    private fun clearOldData() {
        val currentTime = System.currentTimeMillis()
        touchPoints.removeAll { currentTime - it.timestamp > 3000 }
        gesturePaths.removeAll { currentTime - it.startTime > 3500 }
    }
    
    /**
     * Renders the gesture overlay visuals (swipe paths, scroll paths, and touch indicators) onto the provided canvas.
     *
     * Draws swipe and scroll paths with distinct colors and stroke widths, and renders touch indicators:
     * - Taps as a pink expanding/fading ripple with a persistent center dot (2 second fade-out).
     * - Swipe start as a green marker labeled "START".
     * - Swipe end as a red marker labeled "END".
     * - Scroll indicators as blue dots.
     *
     * @param canvas The canvas to draw the gesture overlays on.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw gesture paths (swipes, scrolls)
        for (gesturePath in gesturePaths) {
            when (gesturePath.type) {
                GestureType.SWIPE -> {
                    paint.color = Color.parseColor("#00BCD4") // Cyan
                    paint.strokeWidth = 8f
                    canvas.drawPath(gesturePath.path, paint)
                }
                GestureType.SCROLL -> {
                    paint.color = Color.parseColor("#4CAF50") // Green
                    paint.strokeWidth = 6f
                    canvas.drawPath(gesturePath.path, paint)
                }
                GestureType.TAP -> {
                    // Taps are drawn as circles below
                }
            }
        }
        
        // Draw touch points
        for (point in touchPoints) {
            when (point.type) {
                TouchType.TAP -> {
                    // Draw ripple effect
                    paint.color = Color.parseColor("#FF4081") // Pink
                    paint.strokeWidth = 4f
                    fillPaint.color = Color.parseColor("#FF4081")
                    
                    // Draw expanding circle
                    val elapsed = System.currentTimeMillis() - point.timestamp
                    val maxRadius = 60f
                    val progress = elapsed / 2000f // 2 second animation
                    val currentRadius = maxRadius * progress
                    val alpha = (255 * (1 - progress)).toInt()
                    
                    paint.alpha = alpha
                    fillPaint.alpha = alpha / 2
                    
                    canvas.drawCircle(point.x, point.y, currentRadius, paint)
                    canvas.drawCircle(point.x, point.y, currentRadius * 0.5f, fillPaint)
                    
                    // Draw center dot
                    fillPaint.alpha = 255
                    canvas.drawCircle(point.x, point.y, 8f, fillPaint)
                }
                
                TouchType.SWIPE_START -> {
                    // Draw start point
                    paint.color = Color.parseColor("#4CAF50") // Green
                    paint.strokeWidth = 6f
                    fillPaint.color = Color.parseColor("#4CAF50")
                    
                    canvas.drawCircle(point.x, point.y, 20f, paint)
                    canvas.drawCircle(point.x, point.y, 12f, fillPaint)
                    
                    // Draw "START" text
                    canvas.drawText("START", point.x, point.y - 30f, textPaint)
                }
                
                TouchType.SWIPE_END -> {
                    // Draw end point
                    paint.color = Color.parseColor("#F44336") // Red
                    paint.strokeWidth = 6f
                    fillPaint.color = Color.parseColor("#F44336")
                    
                    canvas.drawCircle(point.x, point.y, 20f, paint)
                    canvas.drawCircle(point.x, point.y, 12f, fillPaint)
                    
                    // Draw "END" text
                    canvas.drawText("END", point.x, point.y - 30f, textPaint)
                }
                
                TouchType.SCROLL -> {
                    // Draw scroll indicator
                    paint.color = Color.parseColor("#2196F3") // Blue
                    paint.strokeWidth = 4f
                    
                    // Draw arrow indicating scroll direction
                    canvas.drawCircle(point.x, point.y, 10f, paint)
                }
            }
        }
    }
    
    enum class ScrollDirection {
        UP, DOWN, LEFT, RIGHT
    }
    
    /**
     * Cleans up resources when the view is detached from the window.
     *
     * Cancels the view's internal CoroutineScope to stop scheduled gesture removals and pending coroutines.
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel() // Clean up coroutines
    }
}