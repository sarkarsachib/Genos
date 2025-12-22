package com.example.androidproject

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.androidproject.accessibility.MyAccessibilityService
import com.example.androidproject.vision.ScreenCaptureCoordinator
import com.example.androidproject.vision.ScreenStateResult
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }

    private var screenCaptureCoordinator: ScreenCaptureCoordinator? = null
    private var accessibilityService: MyAccessibilityService? = null
    
    // Permission request launchers
    private val screenCapturePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            data?.let {
                screenCaptureCoordinator?.onScreenCaptureConsentResult(result.resultCode, it)
            }
        } else {
            Log.w(TAG, "Screen capture permission denied")
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val accessibilityPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check if permission was granted after user returns from settings
        if (isAccessibilityServiceEnabled()) {
            initializeScreenCapture()
        } else {
            Toast.makeText(this, "Accessibility permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface {
                    MainScreen(
                        onStartCapture = { startScreenCapture() },
                        onStopCapture = { stopScreenCapture() },
                        onRequestPermissions = { requestPermissions() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if we need to re-initialize after returning from permissions
        if (isAccessibilityServiceEnabled()) {
            initializeScreenCapture()
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause capture when activity is not visible
        screenCaptureCoordinator?.stopPipeline()
    }

    override fun onDestroy() {
        super.onDestroy()
        screenCaptureCoordinator?.cleanup()
    }

    private fun startScreenCapture() {
        if (isAccessibilityServiceEnabled()) {
            initializeScreenCapture()
        } else {
            requestAccessibilityPermission()
        }
    }

    private fun stopScreenCapture() {
        screenCaptureCoordinator?.stopPipeline()
        Toast.makeText(this, "Screen capture stopped", Toast.LENGTH_SHORT).show()
    }

    private fun requestPermissions() {
        if (!isAccessibilityServiceEnabled()) {
            requestAccessibilityPermission()
        } else {
            requestScreenCaptureConsent()
        }
    }

    private fun initializeScreenCapture() {
        if (screenCaptureCoordinator == null) {
            screenCaptureCoordinator = ScreenCaptureCoordinator(this, this)
            screenCaptureCoordinator?.setAccessibilityService(accessibilityService)
        }
        
        lifecycleScope.launch {
            val initialized = screenCaptureCoordinator?.initializeScreenCapture()
            if (initialized == true) {
                Toast.makeText(this@MainActivity, "Screen capture initialized", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Failed to initialize screen capture", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        accessibilityPermissionLauncher.launch(intent)
    }

    private fun requestScreenCaptureConsent() {
        screenCaptureCoordinator?.let { coordinator ->
            coordinator.requestScreenCaptureConsent()
            // The actual consent request will be handled by the coordinator
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        return enabledServices?.contains(packageName) == true
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun MainScreen(
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    var isCapturing by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Text(
            text = "Screen Capture & OCR Demo",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = "This app demonstrates screen capture with OCR processing",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Button(
            onClick = onRequestPermissions,
            enabled = !isCapturing
        ) {
            Text("Request Permissions")
        }
        
        Button(
            onClick = {
                isCapturing = true
                onStartCapture()
            },
            enabled = !isCapturing
        ) {
            Text("Start Screen Capture")
        }
        
        Button(
            onClick = {
                isCapturing = false
                onStopCapture()
            },
            enabled = isCapturing
        ) {
            Text("Stop Screen Capture")
        }
        
        lastResult?.let { result ->
            Text(
                text = "Last Result: $result",
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Text(
            text = "Features:\n" +
                   "• MediaProjection API for screen capture\n" +
                   "• ML Kit OCR for text recognition\n" +
                   "• Accessibility tree integration\n" +
                   "• Screen state aggregation\n" +
                   "• Lifecycle management",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 32.dp)
        )
    }
}
