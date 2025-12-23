package ai.genos.core.accessibility

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service for managing GENOS accessibility service lifecycle
 */
class GenosForegroundService : Service() {
    
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "genos_foreground_channel"
        private const val NOTIFICATION_ID = 1002
        private const val TAG = "GenosForegroundService"
        
        fun startService(context: Context) {
            val intent = Intent(context, GenosForegroundService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, GenosForegroundService::class.java)
            context.stopService(intent)
        }
    }
    
    private var serviceThread: Thread? = null
    private var isRunning = false
    private lateinit var serviceManager: AccessibilityServiceManager
    
    override fun onCreate() {
        super.onCreate()
        Logger.logInfo(TAG, "Foreground service created")
        
        serviceManager = GenosAccessibilityService.getServiceManager()
        
        // Create notification channel
        createNotificationChannel()
        
        // Start service thread
        startServiceThread()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.logInfo(TAG, "Foreground service started")
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createServiceNotification())
        
        // Update service manager state
        serviceManager.updateServiceState(true)
        
        return START_STICKY // Restart service if killed by system
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }
    
    override fun onDestroy() {
        Logger.logInfo(TAG, "Foreground service destroyed")
        
        // Update service manager state
        serviceManager.updateServiceState(false)
        
        // Stop service thread
        stopServiceThread()
        
        super.onDestroy()
    }
    
    private fun startServiceThread() {
        if (isRunning) return
        
        isRunning = true
        serviceThread = Thread {
            try {
                Logger.logInfo(TAG, "Service thread started")
                
                while (isRunning) {
                    try {
                        // Monitor service health
                        val service = GenosAccessibilityService.getInstance()
                        
                        if (service != null && service.isServiceStarted.get()) {
                            // Service is running normally
                            Thread.sleep(5000) // Check every 5 seconds
                        } else {
                            // Service may have crashed, attempt to restart
                            Logger.logWarning(TAG, "Accessibility service not running, attempting restart")
                            Thread.sleep(10000) // Wait before attempting restart
                        }
                        
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    } catch (e: Exception) {
                        Logger.logError(TAG, "Error in service thread", e)
                        Thread.sleep(5000) // Wait before retrying
                    }
                }
                
            } catch (e: Exception) {
                Logger.logError(TAG, "Service thread crashed", e)
            } finally {
                Logger.logInfo(TAG, "Service thread stopped")
            }
        }
        
        serviceThread?.start()
    }
    
    private fun stopServiceThread() {
        isRunning = false
        serviceThread?.interrupt()
        serviceThread?.join(5000) // Wait up to 5 seconds for thread to stop
        serviceThread = null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "GENOS Accessibility Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service for GENOS accessibility monitoring"
                setShowBadge(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createServiceNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) 
                PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        val stopIntent = Intent(this, MainActivity::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopPendingIntent = PendingIntent.getActivity(
            this, 1, stopIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) 
                PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("GENOS Accessibility Service")
            .setContentText("Monitoring UI interactions and app transitions")
            .setSmallIcon(R.drawable.ic_accessibility_service)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Service",
                stopPendingIntent
            )
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                .bigText("GENOS is monitoring accessibility events and app transitions. " +
                        "This service runs in the background to provide UI tree extraction and context tracking."))
            .build()
    }
}