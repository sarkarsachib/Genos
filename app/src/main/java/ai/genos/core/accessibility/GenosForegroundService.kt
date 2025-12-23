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
        
        /**
         * Starts the GenosForegroundService as a foreground service.
         *
         * @param context Context used to start the service.
         */
        fun startService(context: Context) {
            val intent = Intent(context, GenosForegroundService::class.java)
            context.startForegroundService(intent)
        }
        
        /**
         * Stops the GenosForegroundService if it is currently running.
         *
         * @param context Context used to request the service stop.
         */
        fun stopService(context: Context) {
            val intent = Intent(context, GenosForegroundService::class.java)
            context.stopService(intent)
        }
    }
    
    private var serviceThread: Thread? = null
    private var isRunning = false
    private lateinit var serviceManager: AccessibilityServiceManager
    
    /**
     * Initializes the foreground service's runtime state and starts its background monitoring.
     *
     * Initializes the accessibility service manager, ensures the notification channel exists,
     * and starts the internal thread that monitors the accessibility service lifecycle.
     */
    override fun onCreate() {
        super.onCreate()
        Logger.logInfo(TAG, "Foreground service created")
        
        serviceManager = GenosAccessibilityService.getServiceManager()
        
        // Create notification channel
        createNotificationChannel()
        
        // Start service thread
        startServiceThread()
    }
    
    /**
     * Promotes this service to a foreground service with its notification and marks the accessibility service as running.
     *
     * The method starts the foreground notification and updates the internal service manager state.
     *
     * @return `START_STICKY` so the system will attempt to restart the service if it is killed. 
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.logInfo(TAG, "Foreground service started")
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createServiceNotification())
        
        // Update service manager state
        serviceManager.updateServiceState(true)
        
        return START_STICKY // Restart service if killed by system
    }
    
    /**
     * Indicates that this service does not support binding by clients.
     *
     * @return `null` since the service is not designed to be bound. 
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }
    
    /**
     * Cleans up the foreground service when it is destroyed.
     *
     * Marks the accessibility service as not running, stops the internal monitoring thread, and performs superclass teardown.
     */
    override fun onDestroy() {
        Logger.logInfo(TAG, "Foreground service destroyed")
        
        // Update service manager state
        serviceManager.updateServiceState(false)
        
        // Stop service thread
        stopServiceThread()
        
        super.onDestroy()
    }
    
    /**
     * Starts a background thread that monitors the GENOS accessibility service and attempts recovery if it stops.
     *
     * This sets `isRunning` to true, creates and starts `serviceThread`, and repeatedly checks the
     * accessibility service instance; it handles interruptions and logs unexpected errors. If the thread
     * is already running, the call is a no-op.
     */
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
    
    /**
     * Stops the internal monitoring thread by signalling it to stop, interrupting it, waiting up to 5 seconds for it to terminate, and clearing its reference.
     */
    private fun stopServiceThread() {
        isRunning = false
        serviceThread?.interrupt()
        serviceThread?.join(5000) // Wait up to 5 seconds for thread to stop
        serviceThread = null
    }
    
    /**
     * Creates and registers the notification channel used by the foreground service on Android O and above.
     *
     * The channel is created with low importance, no badge, and no sound; it is ignored on pre-O devices.
     */
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
    
    /**
     * Builds the foreground notification used to promote the service.
     *
     * The notification contains a main intent to open the app, a "Stop Service" action intent,
     * a title, description, small icon, low priority, ongoing flag, and an expanded big-text style.
     *
     * @return The Notification configured for use as the service's foreground notification.
     */
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