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
         * Starts the GenosForegroundService as a foreground service using the provided Context.
         */
        fun startService(context: Context) {
            val intent = Intent(context, GenosForegroundService::class.java)
            context.startForegroundService(intent)
        }
        
        /**
         * Stops the Genos foreground service if it is running.
         *
         * @param context Context used to build and send the stop intent for the service.
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
     * Initializes the foreground service components and begins background monitoring.
     *
     * Obtains the GENOS accessibility service manager, creates the notification channel for
     * the foreground notification, and starts the internal service thread that monitors
     * the accessibility service lifecycle.
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
     * Promotes the service to a foreground service and marks the accessibility manager as running.
     *
     * @param intent The start intent supplied to the service, or null if none was provided.
     * @param flags Additional data about this start request provided by the system.
     * @param startId A unique integer representing this specific start request.
     * @return `START_STICKY` to request the system restart the service if it is killed. 
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
     * Indicates that this service does not support binding.
     *
     * @return `null` because the service does not support binding.
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }
    
    /**
     * Performs shutdown and cleanup when the foreground service is destroyed.
     *
     * Updates the GENOS accessibility service manager to indicate the service is not running,
     * stops the internal monitoring thread, logs the destruction, and delegates to the base
     * implementation.
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
     * Starts a background thread that monitors the GENOS accessibility service and manages its lifecycle.
     *
     * If the monitoring thread is already running this is a no-op. The created thread sets `isRunning`
     * to true, repeatedly checks the current accessibility service instance and its `isServiceStarted`
     * state, and logs or waits before retrying when the service appears stopped. The thread responds to
     * interruptions and logs unexpected exceptions before exiting. The thread reference is stored in
     * `serviceThread` and is started before this function returns.
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
     * Stops the internal monitoring thread used by the service.
     *
     * Signals the thread to exit, interrupts it, waits up to 5 seconds for termination, and clears its reference.
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
     * The channel is configured with low importance, no sound, and badges disabled. On devices older than
     * Android O this function is a no-op.
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
     * Builds the persistent foreground notification used to promote the GENOS accessibility service.
     *
     * The notification contains a content intent that opens MainActivity, an action to stop the service,
     * a descriptive title and text, a small accessibility icon, low priority, and an ongoing flag suitable
     * for use with startForeground.
     *
     * @return A configured Notification ready to be passed to startForeground. */
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