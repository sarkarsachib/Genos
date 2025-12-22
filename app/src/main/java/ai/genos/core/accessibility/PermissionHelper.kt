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
     * Determines whether the app's Genos accessibility service is listed among enabled accessibility services.
     *
     * @return `true` if "ai.genos.core.accessibility.GenosAccessibilityService" for this package is enabled, `false` otherwise.
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
     * Determines whether the accessibility service for the specified package is enabled and currently running.
     *
     * @param context Context used to read system settings.
     * @param packageName The package name whose accessibility service state to check.
     * @return `true` if the accessibility service is enabled and actively started for the package, `false` otherwise.
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
     * Determine whether the app has both the accessibility service enabled and overlay permission.
     *
     * For Android M (API 23) and above this verifies the SYSTEM_ALERT_WINDOW overlay permission; on earlier
     * Android versions the overlay check is assumed to be granted.
     *
     * @return `true` if the accessibility service is enabled and overlay permission is granted, `false` otherwise.
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
     * Opens the system Accessibility Settings so the user can enable this app's accessibility service.
     *
     * @param activity Activity used to launch the settings screen.
     * @param onResult Optional callback that is invoked immediately with `false` because a direct result is not provided by the settings activity; callers should re-check permission state when the activity resumes.
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
     * Opens the system overlay-permission settings for this app when the permission is not granted.
     *
     * If the device is running Android M (API 23) or higher and the app does not have the
     * "draw over other apps" permission, this starts the system settings screen where the user
     * can grant that permission. Does nothing on older Android versions or when the permission
     * is already granted.
     *
     * @param activity The activity used to start the settings activity.
     * @param requestCode The request code passed to startActivityForResult for result handling. Default is 1001.
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
     * Display a non-cancelable dialog that guides the user to enable the app's accessibility service.
     *
     * The dialog explains why the accessibility permission is needed. Selecting "Enable" opens the
     * system Accessibility Settings; selecting "Cancel" shows a brief toast informing the user that
     * the accessibility service is required.
     *
     * @param activity Activity used to host the dialog and to start the accessibility settings intent.
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
     * Display a modal dialog that summarizes the app's accessibility service and overlay permission status.
     *
     * The dialog shows whether the GENOS accessibility service is enabled and whether the app has overlay
     * (draw over other apps) permission. It provides actions to dismiss or, if the accessibility service
     * is disabled, open the system accessibility settings to enable it.
     *
     * @param activity The Activity used to present the dialog and to launch the accessibility settings when requested.
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
     * Create a snapshot of the app's current accessibility and overlay permission states.
     *
     * @return A `PermissionStatus` whose fields are:
     * - `accessibilityServiceEnabled`: `true` if the app's accessibility service is enabled.
     * - `overlayPermissionGranted`: `true` if the app can draw overlays (assumed `true` on API < 23).
     * - `allPermissionsGranted`: `true` if all required permissions are granted.
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
     * Indicates whether the app should present a permission rationale to the user.
     *
     * Checks whether the GENOS accessibility service is disabled for the given activity; if so, a rationale may be appropriate.
     *
     * @param activity The Activity used to determine the current accessibility service state.
     * @return `true` if the accessibility service is not enabled and a rationale should be shown, `false` otherwise.
     */
    fun shouldShowPermissionRationale(activity: Activity): Boolean {
        return !isAccessibilityServiceEnabled(activity)
    }
    
    /**
     * Processes an activity result related to permission requests and handles the overlay permission response.
     *
     * Currently recognizes request code 1001 (overlay permission) and logs that the overlay permission request completed;
     * other request codes are ignored.
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult.
     * @param resultCode The integer result code returned by the child activity.
     * @param data Optional intent data returned by the child activity.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            1001 -> { // Overlay permission request
                Logger.logDebug(TAG, "Overlay permission request completed")
            }
        }
    }
    
    /**
     * Checks accessibility and overlay permission status when an activity resumes and logs the result.
     *
     * Logs the current accessibility service enabled state and, if enabled, logs that all required
     * permissions are granted and the service can start.
     *
     * @param activity The activity used to obtain current permission state. Should be called from the
     * activity's onResume lifecycle callback.
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
     * Creates an Intent that opens the system Accessibility settings screen.
     *
     * @return An Intent that launches the device's Accessibility settings.
     */
    fun getAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
    
    /**
     * Create an intent that opens the system overlay ("draw over other apps") permission screen for the specified package.
     *
     * @param packageName The package name whose overlay permission settings should be shown.
     * @return An Intent targeting the system overlay permission settings for the given package.
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