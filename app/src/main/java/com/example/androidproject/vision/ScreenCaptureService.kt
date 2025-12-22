package com.example.androidproject.vision

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.androidproject.R

class ScreenCaptureService : Service() {

    /**
     * Indicates that this service does not support binding.
     *
     * @return `null` because clients cannot bind to this service.
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Starts the service in the foreground with a notification and ensures its notification channel exists.
     *
     * Creates the notification channel (for Android O+), builds the foreground notification, and starts the service
     * in the foreground. On Android 14+ (API 34+) the service is started with the media projection foreground service type.
     *
     * @return `START_NOT_STICKY` so the system does not recreate the service after it is killed. 
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Capture Service")
            .setContentText("Running...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        // FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION is required for Android 14
        if (Build.VERSION.SDK_INT >= 34) {
             startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
             startForeground(1, notification)
        }

        return START_NOT_STICKY
    }

    /**
     * Ensures the notification channel used by the service exists on Android O (API 26) and higher.
     *
     * When the platform supports notification channels, registers a channel named
     * "Screen Capture Service Channel" with importance IMPORTANCE_DEFAULT using the service's
     * CHANNEL_ID.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "ScreenCaptureServiceChannel"
    }
}