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
     * Performs application startup tasks: establishes the global application instance, configures logging, and runs other initialization steps.
     *
     * This sets the companion `instance` to the application object, records an informational startup message, and initializes file-based logging.
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
     * Initializes the logging subsystem to write logs to files using the application context.
     */
    private fun initializeLogging() {
        Logger.initializeFileLogging(this)
    }
    
    /**
     * Handles application shutdown and records an informational log that the application is terminating.
     */
    override fun onTerminate() {
        super.onTerminate()
        Logger.logInfo("GenosAccessibilityApplication", "Application terminated")
    }
}