package ai.genos.core.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class InputProcessingService : Service() {

    companion object {
        private const val TAG = "InputProcessingService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Input processing service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Input processing service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Input processing service destroyed")
    }
}
