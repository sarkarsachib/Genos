package ai.genos.core.accessibility

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import android.app.Activity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/**
 * Permission helper utilities for accessibility service management
 */
object PermissionHelper {
    
    private const val TAG = "PermissionHelper"
    
    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        val packageName = context.packageName
        val serviceName = "${packageName}/ai.genos.core.accessibility.GenosAccessibilityService"
        
        return enabledServices.split(":").any { it.contains(serviceName) }
    }
    
    /**
     * Check if accessibility service is enabled for specific package
     */
    fun isAccessibilityServiceEnabledForPackage(context: Context, packageName: String): Boolean {
        if (!isAccessibilityServiceEnabled(context)) {
            return false
        }
        
        // Additional check to see if service is actively monitoring
        val service = GenosAccessibilityService.getInstance()
        return service != null && service.isServiceStarted.get()
    }
    
    /**
     * Check if we have required permissions
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        // Check for accessibility service permission (this is handled by the service itself)
        val hasAccessibilityService = isAccessibilityServiceEnabled(context)
        
        // Check for overlay permission (if needed for UI interactions)
        val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        
        return hasAccessibilityService && hasOverlayPermission
    }
    
    /**
     * Request accessibility service to be enabled
     */
    fun requestAccessibilityService(activity: Activity, onResult: ((Boolean) -> Unit)? = null) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        activity.startActivity(intent)
        
        Toast.makeText(
            activity,
            "Please enable GENOS Accessibility Service to continue",
            Toast.LENGTH_LONG
        ).show()
        
        // Note: We can't get a direct result from accessibility settings
        // The calling activity should check the permission in onResume
        onResult?.invoke(false) // Will be updated when user returns
    }
    
    /**
     * Request overlay permission (if needed)
     */
    fun requestOverlayPermission(activity: Activity, requestCode: Int = 1001) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, requestCode)
        }
    }
    
    /**
     * Show accessibility service setup dialog
     */
    fun showAccessibilitySetupDialog(activity: Activity) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Enable Accessibility Service")
            .setMessage(
                "GENOS requires accessibility service permission to monitor UI interactions and app transitions.\n\n" +
                "Would you like to open the accessibility settings to enable it?"
            )
            .setPositiveButton("Enable") { _, _ ->
                requestAccessibilityService(activity)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(activity, "Accessibility service is required", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
    }
    
    /**
     * Show permission status dialog
     */
    fun showPermissionStatusDialog(activity: Activity) {
        val isEnabled = isAccessibilityServiceEnabled(activity)
        val overlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(activity)
        } else {
            true
        }
        
        val status = buildString {
            appendLine("Accessibility Service: ${if (isEnabled) "✅ Enabled" else "❌ Disabled"}")
            appendLine("Overlay Permission: ${if (overlayPermission) "✅ Granted" else "❌ Not Granted"}")
        }
        
        val dialog = AlertDialog.Builder(activity)
            .setTitle("GENOS Permissions Status")
            .setMessage(status)
            .setPositiveButton("OK") { _, _ -> }
            .setNeutralButton("Enable Accessibility") { _, _ ->
                if (!isEnabled) {
                    requestAccessibilityService(activity)
                }
            }
            .create()
        
        dialog.show()
    }
    
    /**
     * Get permission status summary
     */
    fun getPermissionStatus(context: Context): PermissionStatus {
        return PermissionStatus(
            accessibilityServiceEnabled = isAccessibilityServiceEnabled(context),
            overlayPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            },
            allPermissionsGranted = hasRequiredPermissions(context)
        )
    }
    
    /**
     * Check if we should show permission rationale
     */
    fun shouldShowPermissionRationale(activity: Activity): Boolean {
        return !isAccessibilityServiceEnabled(activity)
    }
    
    /**
     * Handle permission result from activity
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            1001 -> { // Overlay permission request
                Logger.logDebug(TAG, "Overlay permission request completed")
            }
        }
    }
    
    /**
     * Monitor permission changes (call this from activity onResume)
     */
    fun onActivityResume(activity: Activity) {
        val isEnabled = isAccessibilityServiceEnabled(activity)
        Logger.logDebug(TAG, "Accessibility service enabled: $isEnabled")
        
        // You can add logic here to handle permission changes
        // For example, start the service if permissions are now granted
        if (isEnabled) {
            // Service can now be started
            Logger.logInfo(TAG, "All permissions granted, service can start")
        }
    }
    
    /**
     * Get accessibility service settings intent
     */
    fun getAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
    
    /**
     * Get overlay permission settings intent
     */
    fun getOverlayPermissionIntent(packageName: String): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:$packageName")
        )
    }
}

// Permission status data class
data class PermissionStatus(
    val accessibilityServiceEnabled: Boolean,
    val overlayPermissionGranted: Boolean,
    val allPermissionsGranted: Boolean
)