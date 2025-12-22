package ai.genos.core.accessibility

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Boot receiver for starting GENOS accessibility service on device boot
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    /**
     * Handles system broadcasts for boot and package-replacement events and starts the GENOS accessibility foreground service when appropriate.
     *
     * Starts GenosForegroundService when the received intent action is ACTION_BOOT_COMPLETED, ACTION_MY_PACKAGE_REPLACED, or ACTION_PACKAGE_REPLACED. Errors during service startup are caught and logged.
     *
     * @param context Context used to start the foreground service.
     * @param intent The received broadcast intent; its `action` determines whether the service is started.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        Logger.logInfo(TAG, "Received broadcast: $action")
        
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Start the accessibility service after boot or app update
                try {
                    val serviceIntent = Intent(context, GenosForegroundService::class.java)
                    context.startForegroundService(serviceIntent)
                    
                    Logger.logInfo(TAG, "Started GENOS accessibility service after $action")
                    
                } catch (e: Exception) {
                    Logger.logError(TAG, "Failed to start service after $action", e)
                }
            }
        }
    }
}