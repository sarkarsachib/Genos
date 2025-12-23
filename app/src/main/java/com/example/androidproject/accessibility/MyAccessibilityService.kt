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
    
    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "AccessibilityService created")
        initializeInputExecutor()
    }

    private fun initializeInputExecutor() {
        try {
            inputExecutor = InputExecutorImpl(this, this)
            Log.d(tag, "InputExecutor initialized successfully")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize InputExecutor", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            Log.d(tag, "Accessibility event: ${AccessibilityEvent.eventTypeToString(it.eventType)}")
        }
    }

    override fun onInterrupt() {
        Log.w(tag, "AccessibilityService interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(tag, "AccessibilityService connected")
        
        // Ensure the service is properly configured for gesture dispatch
        serviceInfo?.let { info ->
            Log.d(tag, "Service capabilities: FLAGS=${info.flags}")
        }
    }

    override fun onDestroy() {
        Log.d(tag, "AccessibilityService destroyed")
        super.onDestroy()
    }

    /**
     * Executes an input command if the service is ready.
     * This method provides access to the InputExecutor capabilities.
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
     * Checks if the service and InputExecutor are ready for command execution.
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
