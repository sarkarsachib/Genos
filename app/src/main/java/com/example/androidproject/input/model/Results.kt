package com.example.androidproject.input.model

/**
 * Sealed class representing the result of an input command execution.
 */
sealed class InputResult {
    /**
     * Success result with an optional message and metadata.
     * 
     * @property message Human-readable success message
     * @property metadata Optional technical details
     */
    data class Success(
        val message: String,
        val metadata: Map<String, Any>? = null
    ) : InputResult()

    /**
     * Failure result with error details.
     * 
     * @property reason Human-readable error description
     * @property error Optional exception or technical error
     * @property errorType Categorized error type
     */
    data class Failure(
        val reason: String,
        val error: Throwable? = null,
        val errorType: ErrorType = ErrorType.UNKNOWN
    ) : InputResult()

    /**
     * Enum defining common error types for input execution failures.
     */
    enum class ErrorType {
        UNKNOWN,
        PERMISSION_DENIED,
        SERVICE_NOT_AVAILABLE,
        INVALID_COORDINATES,
        FOCUS_NOT_FOUND,
        INPUT_METHOD_NOT_ACTIVE,
        GESTURE_DISPATCH_FAILED,
        TIMEOUT,
        INVALID_STATE,
        BOUNDARY_VIOLATION
    }

    /**
     * Convenience method to check if the result represents success.
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Convenience method to check if the result represents failure.
     */
    fun isFailure(): Boolean = this is Failure

    /**
     * Convenience method to get a human-readable description of the result.
     */
    fun description(): String {
        return when (this) {
            is Success -> message
            is Failure -> "$reason${error?.let { " (${it.message})" } ?: ""}"
        }
    }
}