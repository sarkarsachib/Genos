package ai.genos.core.overlay

import android.content.Context
import android.util.Log

class OverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "OverlayManager"
    }

    fun showOverlay() {
        Log.d(TAG, "Show overlay - placeholder implementation")
    }

    fun hideOverlay() {
        Log.d(TAG, "Hide overlay - placeholder implementation")
    }

    fun isOverlayVisible(): Boolean {
        Log.d(TAG, "Check overlay visibility - placeholder implementation")
        return false
    }
}
