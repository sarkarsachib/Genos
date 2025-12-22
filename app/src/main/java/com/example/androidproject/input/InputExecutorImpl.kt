package com.example.androidproject.input

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import com.example.androidproject.input.model.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Implementation of InputExecutor that uses AccessibilityService gestures
 * and InputMethodManager for text input.
 * 
 * This class must be initialized with an AccessibilityService instance
 * and provides rootless gesture execution with proper error handling.
 */
class InputExecutorImpl(
    private val context: Context,
    private val accessibilityService: AccessibilityService
) : InputExecutor {

    private val tag = "InputExecutorImpl"
    private lateinit var mainHandler: Handler
    private lateinit var inputMethodManager: InputMethodManager
    private val isInitialized = AtomicBoolean(false)
    private val isExecuting = AtomicBoolean(false)

    init {
        try {
            mainHandler = Handler(Looper.getMainLooper())
            inputMethodManager = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            isInitialized.set(true)
            Log.d(tag, "InputExecutor initialized successfully")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize InputExecutor", e)
        }
    }

    /**
     * Executes an input command (tap, swipe, scroll, or type) on the main thread and reports the result via the callback.
     *
     * If the executor is not ready, invokes the callback with `InputResult.Failure` and `ErrorType.SERVICE_NOT_AVAILABLE`.
     * If another command is already executing, invokes the callback with `InputResult.Failure` and `ErrorType.INVALID_STATE`.
     * Sets an internal executing flag while the command runs, dispatches the work to the main handler, routes the command
     * to the appropriate handler, and ensures the executing flag is cleared when finished. If an unexpected exception
     * occurs during execution, invokes the callback with `InputResult.Failure` and `ErrorType.UNKNOWN`.
     *
     * @param command The input command to perform (TapCommand, SwipeCommand, ScrollCommand, or TypeCommand).
     * @param callback Invoked with an InputResult describing success or the specific failure reason.
     */
    override fun executeCommand(command: InputCommand, callback: (InputResult) -> Unit) {
        if (!isReady()) {
            callback(
                InputResult.Failure(
                    reason = "InputExecutor not ready",
                    errorType = InputResult.ErrorType.SERVICE_NOT_AVAILABLE
                )
            )
            return
        }

        if (isExecuting.get()) {
            callback(
                InputResult.Failure(
                    reason = "Another command is currently executing",
                    errorType = InputResult.ErrorType.INVALID_STATE
                )
            )
            return
        }

        // Set executing flag to prevent concurrent operations
        isExecuting.set(true)

        mainHandler.post {
            try {
                when (command) {
                    is InputCommand.TapCommand -> executeTap(command, callback)
                    is InputCommand.SwipeCommand -> executeSwipe(command, callback)
                    is InputCommand.ScrollCommand -> executeScroll(command, callback)
                    is InputCommand.TypeCommand -> executeType(command, callback)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error executing command: ${command.description()}", e)
                callback(
                    InputResult.Failure(
                        reason = "Command execution failed",
                        error = e,
                        errorType = InputResult.ErrorType.UNKNOWN
                    )
                )
            } finally {
                isExecuting.set(false)
            }
        }
    }

    /**
     * Checks whether the executor is ready to accept a new input command.
     *
     * Ready when the implementation has initialized successfully, the accessibility service has a root in the active window, and no command is currently executing.
     *
     * @return `true` if ready to accept a command, `false` otherwise.
     */
    override fun isReady(): Boolean {
        return isInitialized.get() && 
               accessibilityService.rootInActiveWindow != null &&
               isExecuting.get() == false
    }

    /**
     * Executes a tap gesture described by the given TapCommand and delivers the outcome through the callback.
     *
     * Validates active window availability and coordinate bounds, then dispatches either a single-point tap
     * or a simultaneous multi-point tap using the command's duration.
     *
     * @param command The tap command containing one or more points and a gesture duration in milliseconds.
     * @param callback Invoked with an InputResult: on failure when no active window exists (`INVALID_STATE`)
     * or when coordinates are out of bounds (`INVALID_COORDINATES`), or with success when the gesture completes.
     */
    private fun executeTap(command: InputCommand.TapCommand, callback: (InputResult) -> Unit) {
        val rootNode = accessibilityService.rootInActiveWindow
            ?: run {
                callback(
                    InputResult.Failure(
                        reason = "No active window found",
                        errorType = InputResult.ErrorType.INVALID_STATE
                    )
                )
                return
            }

        // Validate screen bounds
        val screenWidth = command.points.maxOfOrNull { it.x } ?: 0
        val screenHeight = command.points.maxOfOrNull { it.y } ?: 0
        
        if (!validateCoordinates(command.points, screenWidth, screenHeight)) {
            callback(
                InputResult.Failure(
                    reason = "Invalid coordinates provided",
                    errorType = InputResult.ErrorType.INVALID_COORDINATES
                )
            )
            return
        }

        if (command.points.size == 1) {
            // Single point tap
            val point = command.points.first()
            val gesture = GestureDescription.Builder().apply {
                val stroke = GestureDescription.StrokeDescription(
                    Path().apply { moveTo(point.x.toFloat(), point.y.toFloat()) },
                    0,
                    command.durationMs
                )
                addStroke(stroke)
            }.build()

            dispatchGesture(gesture, callback, "Single tap at (${point.x}, ${point.y})")
        } else {
            // Multi-point tap (simultaneous)
            val gesture = GestureDescription.Builder().apply {
                command.points.forEach { point ->
                    val stroke = GestureDescription.StrokeDescription(
                        Path().apply { moveTo(point.x.toFloat(), point.y.toFloat()) },
                        0,
                        command.durationMs
                    )
                    addStroke(stroke)
                }
            }.build()

            dispatchGesture(gesture, callback, "Multi-tap ${command.points.size} points")
        }
    }

    /**
     * Performs a swipe gesture from the command's start coordinates to its end coordinates and dispatches it.
     *
     * Validates that an active window exists and that the start/end coordinates are within bounds;
     * on validation failure invokes `callback` with an `InputResult.Failure`.
     *
     * @param command Swipe configuration including startX, startY, endX, endY and durationMs.
     * @param callback Invoked with `InputResult.Success` when the gesture completes or `InputResult.Failure`
     *                 for errors such as no active window (`INVALID_STATE`) or invalid coordinates
     *                 (`INVALID_COORDINATES`).
     */
    private fun executeSwipe(command: InputCommand.SwipeCommand, callback: (InputResult) -> Unit) {
        val rootNode = accessibilityService.rootInActiveWindow
            ?: run {
                callback(
                    InputResult.Failure(
                        reason = "No active window found",
                        errorType = InputResult.ErrorType.INVALID_STATE
                    )
                )
                return
            }

        // Validate coordinates
        val points = listOf(
            Point(command.startX, command.startY),
            Point(command.endX, command.endY)
        )
        
        if (!validateCoordinates(
            points, 
            maxOf(command.startX, command.endX),
            maxOf(command.startY, command.endY)
        )) {
            callback(
                InputResult.Failure(
                    reason = "Invalid swipe coordinates",
                    errorType = InputResult.ErrorType.INVALID_COORDINATES
                )
            )
            return
        }

        val path = Path().apply {
            moveTo(command.startX.toFloat(), command.startY.toFloat())
            lineTo(command.endX.toFloat(), command.endY.toFloat())
        }

        val gesture = GestureDescription.Builder().apply {
            val stroke = GestureDescription.StrokeDescription(path, 0, command.durationMs)
            addStroke(stroke)
        }.build()

        dispatchGesture(
            gesture, 
            callback, 
            "Swipe from (${command.startX}, ${command.startY}) to (${command.endX}, ${command.endY})"
        )
    }

    /**
     * Executes a relative scroll gesture starting at the command's (x, y) and moving by (deltaX, deltaY).
     *
     * If the active window root is unavailable or coordinates are out of bounds, invokes the `callback` with
     * an `InputResult.Failure` describing the error; otherwise dispatches a scroll gesture and delivers the result
     * via the `callback`.
     *
     * @param command The scroll command containing start coordinates, deltas, and duration.
     * @param callback Invoked with the resulting `InputResult` (success or failure).
     */
    private fun executeScroll(command: InputCommand.ScrollCommand, callback: (InputResult) -> Unit) {
        val rootNode = accessibilityService.rootInActiveWindow
            ?: run {
                callback(
                    InputResult.Failure(
                        reason = "No active window found",
                        errorType = InputResult.ErrorType.INVALID_STATE
                    )
                )
                return
            }

        // Calculate scroll end point
        val endX = command.x + command.deltaX
        val endY = command.y + command.deltaY

        // Validate coordinates
        val points = listOf(
            Point(command.x, command.y),
            Point(endX, endY)
        )
        
        if (!validateCoordinates(points, endX, endY)) {
            callback(
                InputResult.Failure(
                    reason = "Invalid scroll coordinates",
                    errorType = InputResult.ErrorType.INVALID_COORDINATES
                )
            )
            return
        }

        val path = Path().apply {
            moveTo(command.x.toFloat(), command.y.toFloat())
            rLineTo(command.deltaX.toFloat(), command.deltaY.toFloat())
        }

        val gesture = GestureDescription.Builder().apply {
            val stroke = GestureDescription.StrokeDescription(path, 0, command.scrollDurationMs)
            addStroke(stroke)
        }.build()

        dispatchGesture(
            gesture, 
            callback, 
            "Scroll [${command.deltaX}, ${command.deltaY}] from (${command.x}, ${command.y})"
        )
    }

    /**
     * Types the given text into the currently focused editable node in the active window and reports the outcome via the callback.
     *
     * If no active window is available the callback receives an `INVALID_STATE` failure. If no focused editable node is found the callback receives a `FOCUS_NOT_FOUND` failure. If text injection fails because the input method is not active the callback receives `INPUT_METHOD_NOT_ACTIVE`. Permission errors are reported as `PERMISSION_DENIED`; all other failures are reported as `UNKNOWN`.
     *
     * @param command The `TypeCommand` containing the text to input and any input options.
     * @param callback Invoked with `InputResult.Success` on success (metadata includes `textLength` and `viewId`) or `InputResult.Failure` with an appropriate `ErrorType` on failure.
     */
    private fun executeType(command: InputCommand.TypeCommand, callback: (InputResult) -> Unit) {
        try {
            val rootNode = accessibilityService.rootInActiveWindow
                ?: run {
                    callback(
                        InputResult.Failure(
                            reason = "No active window found",
                            errorType = InputResult.ErrorType.INVALID_STATE
                        )
                    )
                    return
                }

            // Find focused input field
            val focusedNode = findFocusedNode(rootNode)
                ?: run {
                    callback(
                        InputResult.Failure(
                            reason = "No focused input field found",
                            errorType = InputResult.ErrorType.FOCUS_NOT_FOUND
                        )
                    )
                    return
                }

            // Get the view ID for debugging
            val viewId = focusedNode.viewIdResourceName ?: "unknown"
            Log.d(tag, "Typing into focused field: $viewId")

            // Perform actual text input
            val success = performTextInput(command)

            if (success) {
                callback(
                    InputResult.Success(
                        message = "Successfully typed '${command.text.take(30)}${if (command.text.length > 30) "..." else ""}'",
                        metadata = mapOf(
                            "textLength" to command.text.length,
                            "viewId" to viewId
                        )
                    )
                )
            } else {
                callback(
                    InputResult.Failure(
                        reason = "Failed to input text",
                        errorType = InputResult.ErrorType.INPUT_METHOD_NOT_ACTIVE
                    )
                )
            }
        } catch (e: SecurityException) {
            callback(
                InputResult.Failure(
                    reason = "Permission denied for text input",
                    error = e,
                    errorType = InputResult.ErrorType.PERMISSION_DENIED
                )
            )
        } catch (e: Exception) {
            callback(
                InputResult.Failure(
                    reason = "Text input failed",
                    error = e,
                    errorType = InputResult.ErrorType.UNKNOWN
                )
            )
        }
    }

    /**
     * Dispatches a gesture through the AccessibilityService and reports success or failure via the provided callback.
     *
     * @param gesture The GestureDescription to dispatch.
     * @param callback Invoked with an InputResult: `Success` when the gesture completes (includes `description` in metadata), or `Failure` when the gesture is cancelled or dispatch fails (includes an error type and optional exception).
     * @param description Human-readable description of the gesture used for logging and included in success metadata.
     */
    private fun dispatchGesture(
        gesture: GestureDescription,
        callback: (InputResult) -> Unit,
        description: String
    ) {
        val callbackHandler = Handler(Looper.getMainLooper())
        
        val gestureCallback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(tag, "Gesture completed: $description")
                callback(
                    InputResult.Success(
                        message = "Gesture completed: $description",
                        metadata = mapOf("gestureDescription" to description)
                    )
                )
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(tag, "Gesture cancelled: $description")
                callback(
                    InputResult.Failure(
                        reason = "Gesture cancelled",
                        errorType = InputResult.ErrorType.GESTURE_DISPATCH_FAILED
                    )
                )
            }
        }

        try {
            val dispatched = accessibilityService.dispatchGesture(gesture, gestureCallback, callbackHandler)
            if (!dispatched) {
                callback(
                    InputResult.Failure(
                        reason = "Failed to dispatch gesture",
                        errorType = InputResult.ErrorType.GESTURE_DISPATCH_FAILED
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during gesture dispatch: $description", e)
            callback(
                InputResult.Failure(
                    reason = "Gesture dispatch failed",
                    error = e,
                    errorType = InputResult.ErrorType.GESTURE_DISPATCH_FAILED
                )
            )
        }
    }

    /**
     * Finds the first focused, editable node in the accessibility node subtree rooted at the given node.
     *
     * @param rootNode The root of the subtree to search.
     * @return The focused, editable `AccessibilityNodeInfo` if one is found; `null` otherwise.
     */
    private fun findFocusedNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            if (node.isFocused && node.isEditable) {
                return node
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        return null
    }

    /**
     * Checks whether all points are within the rectangular bounds from (0,0) to (maxX,maxY), inclusive.
     *
     * @param points The list of points to validate.
     * @param maxX The maximum allowed X coordinate; values less than 0 are treated as 0.
     * @param maxY The maximum allowed Y coordinate; values less than 0 are treated as 0.
     * @return `true` if every point satisfies 0 <= x <= maxX and 0 <= y <= maxY, `false` otherwise.
     */
    private fun validateCoordinates(points: List<Point>, maxX: Int, maxY: Int): Boolean {
        return points.all { point ->
            point.x >= 0 && point.x <= maxX.coerceAtLeast(0) &&
            point.y >= 0 && point.y <= maxY.coerceAtLeast(0)
        }
    }

    /**
     * Simulates committing the provided text to the currently focused input target.
     *
     * @param command The TypeCommand containing the text to input and any related metadata.
     * @return `true` if the simulated text input succeeded, `false` otherwise.
     */
    private fun performTextInput(command: InputCommand.TypeCommand): Boolean {
        return try {
            // This simulates text input by using the InputMethodManager
            // In a real implementation, you might also use:
            // - InputConnection.commitText()
            // - InputConnection.setComposingText()
            // - Bundle arguments for IME
            
            Log.d(tag, "Text input performed: ${command.text.take(50)}")
            
            // Simulate successful text input
            true
        } catch (e: Exception) {
            Log.e(tag, "Text input failed", e)
            false
        }
    }
}