package com.example.androidproject.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.androidproject.input.InputExecutor
import com.example.androidproject.input.InputExecutorImpl
import com.example.androidproject.input.model.InputCommand
import com.example.androidproject.input.model.InputResult

class MyAccessibilityService : AccessibilityService() {

    private val tag = "MyAccessibilityService"
    private lateinit var inputExecutor: InputExecutor
    
    /**
     * Performs startup for the accessibility service and initializes the input executor.
     *
     * Called when the service is created to set up internal resources required to handle input commands.
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "AccessibilityService created")
        initializeInputExecutor()
    }

    /**
     * Initializes the service's InputExecutor instance.
     *
     * Attempts to construct and assign an InputExecutorImpl to [inputExecutor]. On success logs a debug
     * message; on failure catches the exception, logs an error, and leaves [inputExecutor] uninitialized.
     */
    private fun initializeInputExecutor() {
        try {
            inputExecutor = InputExecutorImpl(this, this)
            Log.d(tag, "InputExecutor initialized successfully")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize InputExecutor", e)
        }
    }

    /**
     * Handles an incoming accessibility event by logging its event type.
     *
     * @param event The received AccessibilityEvent; if `null`, the event is ignored.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            Log.d(tag, "Accessibility event: ${AccessibilityEvent.eventTypeToString(it.eventType)}")
        }
    }

    /**
     * Handles an interruption of the accessibility service.
     *
     * Logs a warning indicating the service has been interrupted.
     */
    override fun onInterrupt() {
        Log.w(tag, "AccessibilityService interrupted")
    }

    /**
     * Called when the accessibility service is connected; logs the connection and available service capabilities.
     *
     * When the service's configuration is available (`serviceInfo`), logs its flags to aid in diagnosing gesture
     * and capability settings.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(tag, "AccessibilityService connected")
        
        // Ensure the service is properly configured for gesture dispatch
        serviceInfo?.let { info ->
            Log.d(tag, "Service capabilities: FLAGS=${info.flags}")
        }
    }

    /**
     * Handles the accessibility service being destroyed.
     *
     * Logs the destruction and invokes the superclass teardown.
     */
    override fun onDestroy() {
        Log.d(tag, "AccessibilityService destroyed")
        super.onDestroy()
    }

    /**
     * Execute an input command using the service's input executor.
     *
     * If the executor is ready, the command is executed and the provided callback
     * receives the resulting `InputResult`. If the executor is not ready, the
     * callback is invoked with an `InputResult.Failure` whose reason is
     * "Accessibility service not ready for input execution" and `errorType` is
     * `InputResult.ErrorType.SERVICE_NOT_AVAILABLE`.
     *
     * @param command The input command to execute.
     * @param callback Receives the `InputResult` produced by executing the command.
     */
    fun executeInputCommand(command: InputCommand, callback: (InputResult) -> Unit) {
        if (!::inputExecutor.isInitialized) {
            inputExecutor = InputExecutorImpl(this, this)
        }

        if (inputExecutor.isReady()) {
            inputExecutor.executeCommand(command, callback)
        } else {
            callback(
                InputResult.Failure(
                    reason = "Accessibility service not ready for input execution",
                    errorType = InputResult.ErrorType.SERVICE_NOT_AVAILABLE
                )
            )
        }
    }

    /**
     * Reports whether the accessibility service is prepared to execute input commands.
     *
     * Returns `false` if the `InputExecutor` has not been initialized or is not ready.
     *
     * @return `true` if the service and its `InputExecutor` are ready to accept commands, `false` otherwise.
     */
    fun isInputReady(): Boolean {
        return if (::inputExecutor.isInitialized) {
            inputExecutor.isReady()
        } else {
            false
        }
    }

    companion object {
        const val ACTION_INPUT_COMMAND = "com.example.androidproject.action.INPUT_COMMAND"
        const val EXTRA_COMMAND_TYPE = "command_type"
        const val EXTRA_COMMAND_DATA = "command_data"
    }
}