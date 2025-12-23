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
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Logger.logInfo("GenosAccessibilityApplication", "Application started")
        
        // Initialize any global configurations here
        initializeLogging()
    }
    
    private fun initializeLogging() {
        Logger.initializeFileLogging(this)
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Logger.logInfo("GenosAccessibilityApplication", "Application terminated")
    }
}