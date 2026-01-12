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