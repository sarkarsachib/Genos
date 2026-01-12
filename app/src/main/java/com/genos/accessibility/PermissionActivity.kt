package com.genos.accessibility

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.genos.accessibility.R
import com.genos.accessibility.model.UiElement
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PermissionActivity : AppCompatActivity() {
    
    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        private const val LOG_UPDATE_INTERVAL = 3000L
    }
    
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var tvOverlayStatus: TextView
    private lateinit var ivAccessibilityStatus: ImageView
    private lateinit var ivOverlayStatus: ImageView
    private lateinit var btnEnableAccessibility: Button
    private lateinit var btnEnableOverlay: Button
    private lateinit var btnStartService: Button
    private lateinit var tvLogs: TextView
    
    private val serviceConnectionHelper by lazy { ServiceConnectionHelper() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        
        initializeViews()
        setupClickListeners()
        updatePermissionStatus()
    }
    
    private fun initializeViews() {
        tvAccessibilityStatus = findViewById(R.id.tv_accessibility_status)
        tvOverlayStatus = findViewById(R.id.tv_overlay_status)
        ivAccessibilityStatus = findViewById(R.id.iv_accessibility_status)
        ivOverlayStatus = findViewById(R.id.iv_overlay_status)
        btnEnableAccessibility = findViewById(R.id.btn_enable_accessibility)
        btnEnableOverlay = findViewById(R.id.btn_enable_overlay)
        btnStartService = findViewById(R.id.btn_start_service)
        tvLogs = findViewById(R.id.tv_logs)
        
        tvLogs.setHorizontallyScrolling(true)
    }
    
    private fun setupClickListeners() {
        btnEnableAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }
        
        btnEnableOverlay.setOnClickListener {
            requestOverlayPermission()
        }
        
        btnStartService.setOnClickListener {
            startAccessibilityService()
        }
    }
    
    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            logMessage("Error opening accessibility settings: ${e.message}")
        }
    }
    
    private fun requestOverlayPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        } catch (e: Exception) {
            logMessage("Error requesting overlay permission: ${e.message}")
        }
    }
    
    private fun startAccessibilityService() {
        logMessage("Accessibility service will be started via Android system")
    }
    
    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        startLogUpdateLoop()
    }
    
    override fun onPause() {
        super.onPause()
        stopLogUpdateLoop()
    }
    
    private fun updatePermissionStatus() {
        val accessibilityEnabled = PermissionChecker.isAccessibilityServiceEnabled(this)
        val overlayEnabled = PermissionChecker.canDrawOverlays(this)
        
        val allPermissionsGranted = accessibilityEnabled && overlayEnabled
        
        updateStatusView(
            isEnabled = accessibilityEnabled,
            textView = tvAccessibilityStatus,
            imageView = ivAccessibilityStatus,
            button = btnEnableAccessibility
        )
        
        updateStatusView(
            isEnabled = overlayEnabled,
            textView = tvOverlayStatus,
            imageView = ivOverlayStatus,
            button = btnEnableOverlay
        )
        
        btnStartService.isEnabled = allPermissionsGranted
        
        if (allPermissionsGranted) {
            btnStartService.text = "All Ready! Enable in Settings"
        } else {
            btnStartService.text = "Grant All Permissions First"
        }
    }
    
    private fun updateStatusView(
        isEnabled: Boolean,
        textView: TextView,
        imageView: ImageView,
        button: Button
    ) {
        if (isEnabled) {
            textView.text = "Enabled"
            button.text = "Disable"
            button.setOnClickListener {
                if (textView == tvAccessibilityStatus) {
                    openAccessibilitySettings()
                } else {
                    openOverlaySettings()
                }
            }
        } else {
            textView.text = "Disabled"
            button.text = "Enable"
            button.setOnClickListener {
                if (textView == tvAccessibilityStatus) {
                    openAccessibilitySettings()
                } else {
                    requestOverlayPermission()
                }
            }
        }
    }
    
    private fun openOverlaySettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (e: Exception) {
            logMessage("Error opening overlay settings: ${e.message}")
        }
    }
    
    private fun logMessage(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logMessage = "[$timestamp] $message\n"
        
        runOnUiThread {
            val currentText = tvLogs.text.toString()
            tvLogs.text = currentText + logMessage
            tvLogs.scrollTo(0, tvLogs.bottom)
        }
    }
    
    private var logUpdateJob: kotlinx.coroutines.Job? = null
    
    private fun startLogUpdateLoop() {
        if (logUpdateJob?.isActive == true) return
        
        logUpdateJob = lifecycleScope.launch {
            while (isActive && !isFinishing) {
                updateLogFromService()
                delay(LOG_UPDATE_INTERVAL)
            }
        }
    }
    
    private fun stopLogUpdateLoop() {
        logUpdateJob?.cancel()
        logUpdateJob = null
    }
    
    private fun updateLogFromService() {
        serviceConnectionHelper.observeServiceState(this) { service ->
            val currentPackage = service.getCurrentAppPackage()
            val elementCount = service.getCurrentUiTree()?.let { tree ->
                countElements(tree.root)
            } ?: 0
            
            val message = buildString {
                append("Service: running, ")
                currentPackage?.let { append("App: $it, ") }
                append("Elements: $elementCount")
            }
            
            logMessage(message)
        }
    }
    
    private fun countElements(element: UiElement): Int {
        return 1 + element.children.sumOf { countElements(it) }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            updatePermissionStatus()
        }
    }
}