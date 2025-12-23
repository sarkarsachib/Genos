package ai.genos.core.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Overlay service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Overlay service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Overlay service destroyed")
    }
}
