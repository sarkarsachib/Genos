package com.example.androidproject.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class MyAccessibilityService : AccessibilityService() {

    /**
     * Handles accessibility events delivered to this service.
     *
     * Called by the system when an accessibility event occurs so the service can respond
     * (for example, UI changes, view focus changes, or notifications). Implementations
     * should inspect the provided event and take appropriate action.
     *
     * @param event The `AccessibilityEvent` received from the system, or `null` if no event data is available.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events here
    }

    /**
     * Called when the accessibility service is interrupted by the system.
     *
     * Implementations should stop or clean up any ongoing accessibility feedback or operations.
     */
    override fun onInterrupt() {
        // Handle interrupt
    }
}