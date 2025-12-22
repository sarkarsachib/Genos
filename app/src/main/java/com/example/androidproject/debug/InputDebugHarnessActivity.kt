package com.example.androidproject.debug

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.androidproject.input.model.InputCommand
import com.example.androidproject.input.model.InputResult
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Debug harness activity for testing input emulation capabilities.
 * Provides UI controls to test taps, swipes, scrolls, and text input.
 */
class InputDebugHarnessActivity : ComponentActivity() {
    
    private val tag = "InputDebugHarness"
    private lateinit var serviceHandler: Handler
    private val isTesting = AtomicBoolean(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        serviceHandler = Handler(Looper.getMainLooper())
        
        Log.d(tag, "InputDebugHarnessActivity created")
        
        setContent {
            InputDebugHarnessContent(
                onTestTap = { x, y -> testTap(x, y) },
                onTestSwipe = { startX, startY, endX, endY -> testSwipe(startX, startY, endX, endY) },
                onTestScroll = { deltaX, deltaY -> testScroll(deltaX, deltaY) },
                onTestType = { text -> testType(text) }
            )
        }
    }
    
    private fun testTap(x: Int, y: Int) {
        if (isTesting.get()) {
            Log.w(tag, "Test already in progress, please wait")
            return
        }
        
        isTesting.set(true)
        Log.i(tag, "Testing tap at ($x, $y)")
        
        // In a real implementation, you would get the service instance properly
        // For now, we'll simulate the execution
        serviceHandler.postDelayed({
            val result = InputCommand.TapCommand(x, y)
            Log.d(tag, "Simulated tap command: ${result.description()}")
            
            val simulatedResult = com.example.androidproject.input.model.InputResult.Success(
                message = "Simulated tap at ($x, $y)",
                metadata = mapOf("coordinates" to "($x, $y)", "type" to "tap")
            )
            
            handleTestResult("Tap", simulatedResult)
            isTesting.set(false)
        }, 500)
    }
    
    private fun testSwipe(startX: Int, startY: Int, endX: Int, endY: Int) {
        if (isTesting.get()) {
            Log.w(tag, "Test already in progress, please wait")
            return
        }
        
        isTesting.set(true)
        Log.i(tag, "Testing swipe from ($startX, $startY) to ($endX, $endY)")
        
        serviceHandler.postDelayed({
            val result = InputCommand.SwipeCommand(startX, startY, endX, endY)
            Log.d(tag, "Simulated swipe command: ${result.description()}")
            
            val simulatedResult = com.example.androidproject.input.model.InputResult.Success(
                message = "Simulated swipe completed",
                metadata = mapOf(
                    "start" to "($startX, $startY)",
                    "end" to "($endX, $endY)",
                    "type" to "swipe"
                )
            )
            
            handleTestResult("Swipe", simulatedResult)
            isTesting.set(false)
        }, 1000)
    }
    
    private fun testScroll(deltaX: Int, deltaY: Int) {
        if (isTesting.get()) {
            Log.w(tag, "Test already in progress, please wait")
            return
        }
        
        isTesting.set(true)
        Log.i(tag, "Testing scroll: [$deltaX, $deltaY]")
        
        serviceHandler.postDelayed({
            val result = InputCommand.ScrollCommand(deltaX, deltaY)
            Log.d(tag, "Simulated scroll command: ${result.description()}")
            
            val simulatedResult = com.example.androidproject.input.model.InputResult.Success(
                message = "Simulated scroll completed",
                metadata = mapOf(
                    "delta" to "[$deltaX, $deltaY]",
                    "type" to "scroll"
                )
            )
            
            handleTestResult("Scroll", simulatedResult)
            isTesting.set(false)
        }, 750)
    }
    
    private fun testType(text: String) {
        if (isTesting.get()) {
            Log.w(tag, "Test already in progress, please wait")
            return
        }
        
        if (text.isBlank()) {
            Log.w(tag, "Cannot type empty text")
            return
        }
        
        isTesting.set(true)
        Log.i(tag, "Testing type with text: ${text.take(30)}")
        
        serviceHandler.postDelayed({
            val result = InputCommand.TypeCommand(text)
            Log.d(tag, "Simulated type command: ${result.description()}")
            
            val simulatedResult = com.example.androidproject.input.model.InputResult.Success(
                message = "Simulated text input completed",
                metadata = mapOf(
                    "textLength" to text.length,
                    "textPreview" to text.take(20),
                    "type" to "type"
                )
            )
            
            handleTestResult("Type", simulatedResult)
            isTesting.set(false)
        }, 1200)
    }
    
    private fun handleTestResult(testType: String, result: com.example.androidproject.input.model.InputResult) {
        when (result) {
            is com.example.androidproject.input.model.InputResult.Success -> {
                Log.i(tag, "$testType test SUCCESS: ${result.message}")
            }
            is com.example.androidproject.input.model.InputResult.Failure -> {
                Log.e(tag, "$testType test FAILED: ${result.reason}", result.error)
            }
        }
    }
    
    companion object {
        const val ACTION_TRIGGER_TEST = "com.example.androidproject.action.TRIGGER_TEST"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputDebugHarnessContent(
    onTestTap: (Int, Int) -> Unit = { _, _ -> },
    onTestSwipe: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onTestScroll: (Int, Int) -> Unit = { _, _ -> },
    onTestType: (String) -> Unit = {}
) {
    var xCoord by remember { mutableStateOf("500") }
    var yCoord by remember { mutableStateOf("800") }
    var startX by remember { mutableStateOf("400") }
    var startY by remember { mutableStateOf("1000") }
    var endX by remember { mutableStateOf("400") }
    var endY by remember { mutableStateOf("500") }
    var deltaX by remember { mutableStateOf("0") }
    var deltaY by remember { mutableStateOf("-300") }
    var text by remember { mutableStateOf("Hello, Input Emulation Engine!") }
    var testStatus by remember { mutableStateOf("Ready") }
    var lastResult by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Input Emulation Engine",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Text(
            text = "Rootless Automation Test Harness",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Divider()
        
        // Status Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Status: $testStatus", style = MaterialTheme.typography.bodyMedium)
                if (lastResult.isNotEmpty()) {
                    Text(
                        text = "Last Result: $lastResult",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        
        Divider()
        
        // Tap Test Controls
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("1. Tap Command", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Simulates touch at specific coordinates",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = xCoord,
                        onValueChange = { xCoord = it },
                        label = { Text("X Coordinates") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = yCoord,
                        onValueChange = { yCoord = it },
                        label = { Text("Y Coordinates") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Button(
                    onClick = { 
                        val x = xCoord.toIntOrNull() ?: 500
                        val y = yCoord.toIntOrNull() ?: 800
                        testStatus = "Executing tap at ($x, $y)..."
                        onTestTap(x, y)
                        lastResult = "Tap command queued"
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("▶ Test Tap")
                }
            }
        }
        
        Divider()
        
        // Swipe Test Controls
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("2. Swipe Command", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Simulates drag gesture with configurable velocity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = startX,
                    onValueChange = { startX = it },
                    label = { Text("Start X") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = startY,
                    onValueChange = { startY = it },
                    label = { Text("Start Y") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = endX,
                    onValueChange = { endX = it },
                    label = { Text("End X") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = endY,
                    onValueChange = { endY = it },
                    label = { Text("End Y") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button(
                    onClick = { 
                        val startXVal = startX.toIntOrNull() ?: 400
                        val startYVal = startY.toIntOrNull() ?: 1000
                        val endXVal = endX.toIntOrNull() ?: 400
                        val endYVal = endY.toIntOrNull() ?: 500
                        testStatus = "Executing swipe $startXVal,$startYVal → $endXVal,$endYVal..."
                        onTestSwipe(startXVal, startYVal, endXVal, endYVal)
                        lastResult = "Swipe command queued"
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("▶ Test Swipe")
                }
            }
        }
        
        Divider()
        
        // Scroll Test Controls
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("3. Scroll Command", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Simulates scrolling gestures",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = deltaX,
                    onValueChange = { deltaX = it },
                    label = { Text("Delta X (Horizontal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = deltaY,
                    onValueChange = { deltaY = it },
                    label = { Text("Delta Y (Vertical)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button(
                    onClick = { 
                        val deltaXVal = deltaX.toIntOrNull() ?: 0
                        val deltaYVal = deltaY.toIntOrNull() ?: -300
                        testStatus = "Executing scroll [$deltaXVal, $deltaYVal]..."
                        onTestScroll(deltaXVal, deltaYVal)
                        lastResult = "Scroll command queued"
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("▶ Test Scroll")
                }
            }
        }
        
        Divider()
        
        // Type Test Controls
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("4. Type Command", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Simulates text input into focused fields",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text to Type") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                Button(
                    onClick = { 
                        testStatus = "Executing type with ${text.length} chars..."
                        onTestType(text)
                        lastResult = "Type command queued"
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("▶ Test Type")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("⚠️ NOTE: Enable Accessibility Service in Settings", style = MaterialTheme.typography.labelMedium)
        Text("Settings → Accessibility → My Accessibility Service", style = MaterialTheme.typography.labelSmall)
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}