package ai.genos.core.accessibility

import android.app.Application
import android.content.Context

/**
 * Application class for GENOS Accessibility
 */
class GenosAccessibilityApplication : Application() {
    
    companion object {
        lateinit var instance: GenosAccessibilityApplication
            private set
    }
    
    /**
     * Perform application-level startup: set the singleton instance, configure logging, and run global initialization.
     *
     * Sets the companion `instance` to this application, emits an informational startup log, and initializes file
     * logging and any other global configuration via `initializeLogging()`.
     */
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Logger.logInfo("GenosAccessibilityApplication", "Application started")
        
        // Initialize any global configurations here
        initializeLogging()
    }
    
    /**
     * Configures file-based logging for the application.
     *
     * Delegates to Logger to initialize logging that writes logs to files using the application context.
     */
    private fun initializeLogging() {
        Logger.initializeFileLogging(this)
    }
    
    /**
     * Logs an informational message when the application is terminating.
     */
    override fun onTerminate() {
        super.onTerminate()
        Logger.logInfo("GenosAccessibilityApplication", "Application terminated")
    }
}