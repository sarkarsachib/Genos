package com.example.androidproject.input

import com.example.androidproject.input.model.InputCommand
import com.example.androidproject.input.model.InputResult

/**
 * Core interface for executing input commands in a rootless environment.
 * Provides a unified API for touch gestures and text input using Android's
 * AccessibilityService and InputMethodManager APIs.
 */
interface InputExecutor {

    /**
     * Executes an input command asynchronously and reports the result via callback.
     * 
     * @param command The InputCommand to execute
     * @param callback Called when execution completes (success or failure)
     */
    fun executeCommand(command: InputCommand, callback: (InputResult) -> Unit)

    /**
     * Checks if the input executor is ready and all required services are available.
     * 
     * @return true if ready for command execution
     */
    fun isReady(): Boolean
}

/**
 * Extension function for mapping command types to human-readable descriptions.
 */
fun InputCommand.description(): String {
    return when (this) {
        is com.example.androidproject.input.model.TapCommand -> 
            "Tap at (${points.joinToString { "(${it.x}, ${it.y})" }})"
        is com.example.androidproject.input.model.SwipeCommand -> 
            "Swipe from (${startX}, ${startY}) to (${endX}, ${endY}) in ${durationMs}ms"
        is com.example.androidproject.input.model.ScrollCommand -> 
            "Scroll [${deltaX}, ${deltaY}] with ${scrollDurationMs}ms duration"
        is com.example.androidproject.input.model.TypeCommand -> 
            "Type text: '${text.take(20)}${if (text.length > 20) "..." else ""}'"
    }
}