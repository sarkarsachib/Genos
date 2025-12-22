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
 * Execute the given InputCommand and deliver the resulting InputResult to the provided callback.
 *
 * @param command The input command to execute.
 * @param callback Invoked with the resulting InputResult when execution completes (success or failure encoded in the InputResult).
 */
    fun executeCommand(command: InputCommand, callback: (InputResult) -> Unit)

    /**
 * Indicates whether the executor is prepared to accept and run input commands.
 *
 * @return `true` if all required services are available and the executor can execute commands, `false` otherwise.
 */
    fun isReady(): Boolean
}

/**
 * Produces a human-readable description of an InputCommand.
 *
 * @return A description string:
 * - Tap: "Tap at ((x1, y1), (x2, y2), ...)"
 * - Swipe: "Swipe from (startX, startY) to (endX, endY) in <durationMs>ms"
 * - Scroll: "Scroll [deltaX, deltaY] with <scrollDurationMs>ms duration"
 * - Type: "Type text: 'textPreview...'" where `textPreview` is the first 20 characters and "..." is appended if the text is longer than 20 characters
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