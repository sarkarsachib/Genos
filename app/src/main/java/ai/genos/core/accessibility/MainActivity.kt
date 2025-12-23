package ai.genos.core.accessibility

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import androidx.appcompat.widget.Toolbar

/**
 * Main activity for GENOS accessibility service
 * Provides UI for enabling/disabling service and viewing status
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var startServiceButton: Button
    private lateinit var stopServiceButton: Button
    private lateinit var permissionButton: Button
    private lateinit var logButton: Button
    
    /**
     * Initializes the activity UI, toolbar, view bindings, click listeners, logger, and refreshes the displayed status.
     *
     * @param savedInstanceState The saved instance state bundle provided by the system, or `null` if none.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupToolbar()
        initializeViews()
        setupClickListeners()
        updateUI()
        
        // Initialize logger
        Logger.initializeFileLogging(this)
        Logger.logInfo("MainActivity", "MainActivity created")
    }
    
    /**
     * Handles activity resume by refreshing the UI state and delegating permission-handling tasks.
     *
     * Calls updateUI() to refresh displayed status and notifies PermissionHelper that the activity resumed
     * so it can continue or finalize any pending permission flows.
     */
    override fun onResume() {
        super.onResume()
        updateUI()
        PermissionHelper.onActivityResume(this)
    }
    
    /**
     * Configures the activity's toolbar as the support action bar and sets its title to "GENOS Accessibility".
     */
    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "GENOS Accessibility"
    }
    
    /**
     * Binds UI widgets from the activity layout to the corresponding properties.
     *
     * Sets the following view fields: `statusText`, `startServiceButton`, `stopServiceButton`,
     * `permissionButton`, and `logButton`.
     */
    private fun initializeViews() {
        statusText = findViewById(R.id.status_text)
        startServiceButton = findViewById(R.id.start_service_button)
        stopServiceButton = findViewById(R.id.stop_service_button)
        permissionButton = findViewById(R.id.permission_button)
        logButton = findViewById(R.id.log_button)
    }
    
    /**
     * Binds click handlers to the activity buttons: starts and stops the accessibility service, shows the permission status dialog, and opens the log viewer.
     */
    private fun setupClickListeners() {
        startServiceButton.setOnClickListener {
            startAccessibilityService()
        }
        
        stopServiceButton.setOnClickListener {
            stopAccessibilityService()
        }
        
        permissionButton.setOnClickListener {
            PermissionHelper.showPermissionStatusDialog(this)
        }
        
        logButton.setOnClickListener {
            showLogs()
        }
    }
    
    /**
     * Starts the GENOS accessibility foreground service when required permissions are present.
     *
     * If required permissions are missing, shows the accessibility setup dialog and returns without starting the service.
     * On success, initiates the foreground service and shows a short toast; on failure logs the error and shows a long error toast.
     */
    private fun startAccessibilityService() {
        if (!PermissionHelper.hasRequiredPermissions(this)) {
            PermissionHelper.showAccessibilitySetupDialog(this)
            return
        }
        
        try {
            // Start the foreground service
            val serviceIntent = Intent(this, GenosForegroundService::class.java)
            startForegroundService(serviceIntent)
            
            Toast.makeText(this, "GENOS Accessibility Service starting...", Toast.LENGTH_SHORT).show()
            Logger.logInfo("MainActivity", "Starting accessibility service")
            
        } catch (e: Exception) {
            Logger.logError("MainActivity", "Failed to start service", e)
            Toast.makeText(this, "Failed to start service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Stops the GenosForegroundService and updates the user with a toast and log entry.
     *
     * Attempts to stop the running GENOS accessibility foreground service; on success shows a short
     * confirmation toast and logs the action, on failure logs the error and shows a long error toast.
     */
    private fun stopAccessibilityService() {
        try {
            val serviceIntent = Intent(this, GenosForegroundService::class.java)
            stopService(serviceIntent)
            
            Toast.makeText(this, "GENOS Accessibility Service stopped", Toast.LENGTH_SHORT).show()
            Logger.logInfo("MainActivity", "Stopped accessibility service")
            
        } catch (e: Exception) {
            Logger.logError("MainActivity", "Failed to stop service", e)
            Toast.makeText(this, "Failed to stop service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Refreshes the activity UI to reflect the current accessibility service and permission state.
     *
     * Updates the statusText with:
     * - Permissions status and whether the service is running
     * - Current app context (package, activity, screen state) when the service is available
     * - Up to five recent transitions
     * - Service statistics (total transitions and formatted last heartbeat)
     *
     * Also enables or disables the start and stop service buttons based on permissions and whether the service is running.
     */
    private fun updateUI() {
        val service = GenosAccessibilityService.getInstance()
        val serviceManager = GenosAccessibilityService.getServiceManager()
        val serviceRunning = serviceManager.isServiceRunning()
        val permissionsGranted = PermissionHelper.hasRequiredPermissions(this)
        
        val statusBuilder = StringBuilder()
        statusBuilder.appendLine("GENOS Accessibility Service Status")
        statusBuilder.appendLine("================================")
        statusBuilder.appendLine()
        statusBuilder.appendLine("Permissions: ${if (permissionsGranted) "✅ Granted" else "❌ Missing"}")
        statusBuilder.appendLine("Service Running: ${if (serviceRunning) "✅ Active" else "❌ Inactive"}")
        
        if (service != null) {
            val context = service.getCurrentContext()
            statusBuilder.appendLine()
            statusBuilder.appendLine("Current App Context:")
            statusBuilder.appendLine("Package: ${context.packageName}")
            statusBuilder.appendLine("Activity: ${context.activityName}")
            statusBuilder.appendLine("Screen On: ${context.isScreenOn}")
            
            // Get recent transitions
            val recentTransitions = service.getRecentTransitions(5)
            if (recentTransitions.isNotEmpty()) {
                statusBuilder.appendLine()
                statusBuilder.appendLine("Recent Transitions:")
                recentTransitions.forEach { transition ->
                    statusBuilder.appendLine("  ${transition.toPackage}/${transition.toActivity}")
                }
            }
            
            // Get service statistics
            val stats = serviceManager.getServiceStatistics()
            statusBuilder.appendLine()
            statusBuilder.appendLine("Service Statistics:")
            statusBuilder.appendLine("Total Transitions: ${stats.totalTransitions}")
            statusBuilder.appendLine("Last Heartbeat: ${java.text.SimpleDateFormat("HH:mm:ss", 
                java.util.Locale.getDefault()).format(java.util.Date(stats.lastHeartbeat))}")
        }
        
        statusText.text = statusBuilder.toString()
        
        // Update button states
        startServiceButton.isEnabled = permissionsGranted && !serviceRunning
        stopServiceButton.isEnabled = serviceRunning
    }
    
    /**
     * Opens the log viewer with current log contents or shows a toast when no logs are available.
     *
     * If logs exist, launches LogActivity and supplies the log text via the "log_contents" intent extra;
     * otherwise displays a short "No logs available" toast.
     */
    private fun showLogs() {
        val logContents = Logger.getLogContents()
        if (logContents != null) {
            val intent = Intent(this, LogActivity::class.java)
            intent.putExtra("log_contents", logContents)
            startActivity(intent)
        } else {
            Toast.makeText(this, "No logs available", Toast.LENGTH_SHORT).show()
        }
    }
}