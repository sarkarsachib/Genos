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
 * Reports whether this instance represents a success result.
 *
 * @return `true` if this instance is of type `Success`, `false` otherwise.
 */
    fun isSuccess(): Boolean = this is Success

    /**
 * Checks whether this result is a Failure.
 *
 * @return `true` if this is a Failure, `false` otherwise.
 */
    fun isFailure(): Boolean = this is Failure

    /**
     * Produces a human-readable description of this input result.
     *
     * For a Success, returns the success message. For a Failure, returns the reason
     * and appends the underlying error's message in parentheses if an error exists.
     *
     * @return The result description: the success message for Success, or the failure
     * reason optionally followed by the error message in parentheses for Failure.
     */
    fun description(): String {
        return when (this) {
            is Success -> message
            is Failure -> "$reason${error?.let { " (${it.message})" } ?: ""}"
        }
    }
}