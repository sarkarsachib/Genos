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
     * Determines whether the GENOS accessibility service is enabled for this app.
     *
     * @param context Context used to access system settings.
     * @return `true` if the GENOS accessibility service is listed among enabled accessibility services, `false` otherwise.
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
     * Determines whether the GENOS accessibility service is enabled and actively running.
     *
     * @param context Context used to read system settings.
     * @param packageName The package to check (currently unused — this function checks the GENOS service instance).
     * @return `true` if the GENOS accessibility service is enabled and its instance reports started, `false` otherwise.
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
     * Determines whether the app has both the GENOS accessibility service enabled and, when applicable, the overlay permission.
     *
     * @param context Android context used to query system settings.
     * @return `true` if the accessibility service is enabled and the overlay permission is granted (overlay check applies only on Android 6.0+), `false` otherwise.
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
     * Opens the system Accessibility Settings and prompts the user to enable the GENOS Accessibility Service.
     *
     * The provided callback cannot receive a confirmed result from the settings screen; it is invoked immediately
     * with `false` to indicate that the enablement status is unknown and must be re-checked (for example, in the
     * calling activity's `onResume`).
     *
     * @param activity The activity used to launch the settings screen and show the prompt.
     * @param onResult Optional callback invoked immediately with `false` to indicate no direct confirmation is available.
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
     * Opens the system overlay permission screen when the app lacks the "draw over other apps" permission on Android M (API 23) and above.
     *
     * @param activity Activity used to start the settings activity.
     * @param requestCode Request code passed to startActivityForResult; used to identify the overlay permission result. Defaults to 1001.
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
     * Display a non-cancelable dialog prompting the user to enable the GENOS accessibility service.
     *
     * The dialog explains why the accessibility permission is required. Selecting the positive action
     * opens the system Accessibility Settings for the app; selecting the negative action shows a
     * short toast informing the user that the accessibility service is required.
     *
     * @param activity Activity used to show the dialog and to navigate to system settings. 
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
     * Display a dialog summarizing the app's Accessibility Service and overlay permission status.
     *
     * The dialog shows the current enabled/disabled state for the Accessibility Service and whether
     * the overlay permission is granted. It provides an "OK" button to dismiss and an
     * "Enable Accessibility" action that opens the accessibility settings if the service is not enabled.
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
     * Get current permission state for accessibility and overlay.
     *
     * @return A PermissionStatus with:
     *  - `accessibilityServiceEnabled`: `true` if the GENOS accessibility service is enabled, `false` otherwise.
     *  - `overlayPermissionGranted`: `true` if the app can draw overlays, `false` otherwise.
     *  - `allPermissionsGranted`: `true` if both required permissions are satisfied, `false` otherwise.
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
     * Determines whether a rationale explaining the accessibility permission should be shown.
     *
     * @param activity Activity used to evaluate the current accessibility service state.
     * @return `true` if the GENOS accessibility service is not enabled and a rationale should be shown, `false` otherwise.
     */
    fun shouldShowPermissionRationale(activity: Activity): Boolean {
        return !isAccessibilityServiceEnabled(activity)
    }
    
    /**
     * Processes activity result callbacks for permission requests and handles known request codes.
     *
     * Currently logs completion of the overlay permission flow when the overlay request (1001) returns.
     *
     * @param requestCode The original request code; 1001 corresponds to the overlay permission request.
     * @param resultCode The result code returned by the activity.
     * @param data Optional intent data returned by the activity.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            1001 -> { // Overlay permission request
                Logger.logDebug(TAG, "Overlay permission request completed")
            }
        }
    }
    
    /**
     * Checks accessibility service enablement when an Activity resumes and logs the current status.
     *
     * Call from Activity.onResume to detect permission changes and allow the app to react (for example, start the service when permissions are granted).
     *
     * @param activity The Activity used to query accessibility settings and provide context for any follow-up actions.
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
     * Create an Intent that opens the system Accessibility Settings screen.
     *
     * @return An Intent targeting the device's Accessibility Settings. 
     */
    fun getAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
    
    /**
     * Opens the system screen to manage the app's "draw over other apps" (overlay) permission.
     *
     * @param packageName The package name of the app whose overlay permission settings should be shown.
     * @return An intent that opens the overlay permission settings for the given package.
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