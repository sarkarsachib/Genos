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
     * Initializes the activity UI, toolbar, view bindings, click listeners, and logger, then refreshes the displayed state.
     *
     * Sets the content view, configures the app bar, binds UI components, wires button handlers, updates the UI to reflect current service and permission state, and initializes file-based logging.
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
     * Refreshes the activity UI and performs permission-related resume handling.
     *
     * Updates visible status information and forwards the resume event to the permission
     * handling component so permission state and related dialogs are re-evaluated.
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
     * Binds the activity's view fields to their corresponding views in the layout.
     *
     * Initializes:
     * - `statusText` -> `R.id.status_text`
     * - `startServiceButton` -> `R.id.start_service_button`
     * - `stopServiceButton` -> `R.id.stop_service_button`
     * - `permissionButton` -> `R.id.permission_button`
     * - `logButton` -> `R.id.log_button`
     */
    private fun initializeViews() {
        statusText = findViewById(R.id.status_text)
        startServiceButton = findViewById(R.id.start_service_button)
        stopServiceButton = findViewById(R.id.stop_service_button)
        permissionButton = findViewById(R.id.permission_button)
        logButton = findViewById(R.id.log_button)
    }
    
    /**
     * Attaches click handlers to the activity buttons to trigger service control, permission dialog, and log viewing.
     *
     * - Start button: starts the accessibility foreground service.
     * - Stop button: stops the accessibility foreground service.
     * - Permission button: shows the permission status dialog.
     * - Log button: opens the log viewer.
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
     * If required permissions are missing, shows the accessibility setup dialog and does nothing else.
     * On successful start, displays a short toast and logs an informational message.
     * On failure, logs the error and shows a long toast with the error message.
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
     * Stops the GenosForegroundService, displays a short Toast to the user, and logs the action.
     *
     * If stopping the service fails, logs the error and displays a long Toast containing the exception message.
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
     * Refreshes the activity UI to reflect the current accessibility service state.
     *
     * Updates the statusText view with permission state, whether the service is running,
     * current app context (package, activity, screen state), up to five recent transitions,
     * and basic service statistics (total transitions and last heartbeat time).
     * Also enables or disables the start and stop service buttons based on permission and running state.
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
     * Opens the log viewer activity with current log contents if available; otherwise shows a short toast.
     *
     * If logs are present, starts LogActivity and supplies the logs as the "log_contents" intent extra.
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