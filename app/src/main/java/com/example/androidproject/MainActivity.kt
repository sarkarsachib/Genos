package com.example.androidproject

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import com.example.androidproject.overlay.OverlayService
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    
    private val permissions = arrayOf(
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.BIND_ACCESSIBILITY_SERVICE
    )
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            launchOverlay()
        } else {
            Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            launchOverlay()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(
                onLaunchOverlay = { checkAndLaunchOverlay() },
                onRequestPermissions = { requestPermissions() },
                onSendCommand = { command -> sendCommand(command) }
            )
        }
    }
    
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(overlayIntent)
        }
        
        // Request other permissions
        ActivityCompat.requestPermissions(this, permissions, 100)
    }
    
    private fun checkAndLaunchOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                Settings.canDrawOverlays(this) -> {
                    launchOverlay()
                }
                else -> {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    overlayPermissionLauncher.launch(intent)
                }
            }
        } else {
            launchOverlay()
        }
    }
    
    private fun launchOverlay() {
        val serviceIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START_MONITORING
        }
        ContextCompat.startForegroundService(this, serviceIntent)
        Toast.makeText(this, "Overlay service started", Toast.LENGTH_SHORT).show()
    }
    
    private fun sendCommand(command: String) {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_UPDATE_STATUS
            putExtra(OverlayService.EXTRA_STATUS, "Command: $command")
        }
        sendBroadcast(intent)
        
        // Example: Parse and execute command
        CoroutineScope(Dispatchers.Main).launch {
            when {
                command.startsWith("tap") -> {
                    val parts = command.split(" ")
                    if (parts.size >= 3) {
                        showTouchVisualization(
                            x = parts[1].toFloatOrNull() ?: 0f,
                            y = parts[2].toFloatOrNull() ?: 0f
                        )
                    }
                }
                command.startsWith("ocr") -> {
                    val ocrIntent = Intent(this@MainActivity, OverlayService::class.java)
                    ocrIntent.action = OverlayService.ACTION_REQUEST_OCR
                    sendBroadcast(ocrIntent)
                }
            }
        }
    }
    
    private fun showTouchVisualization(x: Float, y: Float) {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_TOUCH
            putExtra(OverlayService.EXTRA_X, x)
            putExtra(OverlayService.EXTRA_Y, y)
        }
        sendBroadcast(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLaunchOverlay: () -> Unit,
    onRequestPermissions: () -> Unit,
    onSendCommand: (String) -> Unit
) {
    var commandInput by remember { mutableStateOf("") }
    var statusLog by remember { mutableStateOf("Status Log:\n")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GENOS Overlay System") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Overlay Status",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ready to launch overlay service",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Permission Button
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Overlay & Accessibility Permissions")
            }
            
            // Launch Overlay Button
            Button(
                onClick = onLaunchOverlay,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Launch GENOS Overlay")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Command Console
            Text(
                text = "Command Console",
                style = MaterialTheme.typography.titleMedium
            )
            
            // Command Input
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enter command (e.g., 'tap 500 500' or 'ocr')") },
                singleLine = true
            )
            
            // Command Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { onSendCommand(commandInput) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Send Command")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = { 
                        commandInput = "tap 500 500"
                        onSendCommand("tap 500 500")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test Tap")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = { 
                        commandInput = "ocr"
                        onSendCommand("ocr")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test OCR")
                }
            }
            
            // Status Log
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = statusLog,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}