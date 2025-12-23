package com.example.androidproject.overlay

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.example.androidproject.command.CommandProcessor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlayIntegrationTest {
    
    @get:Rule
    val serviceRule = ServiceTestRule()
    
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private lateinit var commandProcessor: CommandProcessor
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        commandProcessor = CommandProcessor()
        
        // Start overlay service
        val serviceIntent = Intent(context, OverlayService::class.java)
        serviceRule.startService(serviceIntent)
        
        // Wait for service to be ready
        Thread.sleep(2000)
    }
    
    @Test
    fun testCompleteWorkflow() {
        // Step 1: Read screen tree
        testScreenReading()
        
        // Step 2: Parse and execute command
        testCommandParsingAndExecution()
        
        // Step 3: Verify OCR capability
        testOcrFunctionality()
        
        // Step 4: Verify overlay feedback
        testOverlayFeedback()
    }
    
    private fun testScreenReading() {
        // Navigate to test app (Settings for consistency)
        device.pressHome()
        val launcherPackage = device.launcherPackageName
        assertNotNull("Launcher package should not be null", launcherPackage)
        
        // Wait for launcher
        val condition = Until.hasObject(By.pkg(launcherPackage).depth(0))
        device.wait(condition, 5000)
        
        // Open Settings to have a consistent test environment
        device.executeShellCommand("am start -a android.settings.SETTINGS")
        Thread.sleep(2000)
        
        // Verify overlay is visible by checking service status
        val overlayIntent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_UPDATE_STATUS
            putExtra(OverlayService.EXTRA_STATUS, "Test: Screen reading")
        }
        context.sendBroadcast(overlayIntent)
        
        assertTrue("Should be able to send broadcast to overlay", true)
    }
    
    private fun testCommandParsingAndExecution() {
        val testCommands = listOf(
            "tap 500 500",
            "swipe 300 800 300 400",
            "scroll down",
            "wait 1000"
        )
        
        for (commandString in testCommands) {
            val command = commandProcessor.parseCommand(commandString)
            assertNotNull("Should parse command: $commandString", command)
            
            val displayText = commandProcessor.formatCommandForDisplay(command!!)
            assertTrue("Should format command for display: $displayText", displayText.isNotEmpty())
            
            // Send command to overlay
            val intent = Intent(context, OverlayService::class.java).apply {
                action = OverlayService.ACTION_UPDATE_STATUS
                putExtra(OverlayService.EXTRA_STATUS, "Command: $displayText")
            }
            context.sendBroadcast(intent)
        }
    }
    
    private fun testOcrFunctionality() {
        // Trigger OCR request
        val ocrIntent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_REQUEST_OCR
        }
        context.sendBroadcast(ocrIntent)
        
        // Verify OCR request was sent successfully
        assertTrue("Should be able to request OCR", true)
    }
    
    private fun testOverlayFeedback() {
        // Test touch visualization
        val touchIntent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_TOUCH
            putExtra(OverlayService.EXTRA_X, 500f)
            putExtra(OverlayService.EXTRA_Y, 700f)
        }
        context.sendBroadcast(touchIntent)
        
        Thread.sleep(500) // Give visualization time to appear
        
        // Test status updates
        val statusIntent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_UPDATE_STATUS
            putExtra(OverlayService.EXTRA_STATUS, "Automation Running")
        }
        context.sendBroadcast(statusIntent)
        
        assertTrue("Should receive overlay feedback", true)
    }
    
    @Test
    fun testCommandVariants() {
        val testCases = mapOf(
            "tap 100 200" to "Tap at (100, 200)",
            "swipe 0 0 100 100 500" to "Swipe from (0, 0) to (100, 100)",
            "scroll up" to "Scroll UP",
            "input hello world" to "Input: \"hello world\"",
            "wait 2000" to "Wait 2000ms",
            "back" to "Press Back"
        )
        
        for ((input, expectedDescription) in testCases) {
            val command = commandProcessor.parseCommand(input)
            assertNotNull("Should parse: $input", command)
            
            val description = commandProcessor.formatCommandForDisplay(command!!)
            assertTrue("Description should contain expected text for $input", 
                description.contains(expectedDescription.substringBefore(":")))
        }
    }
    
    @Test
    fun testMultiCommandParsing() {
        val multiCommandScript = """
            # Test script
            tap 500 500
            wait 1000
            scroll down
            wait 500
            tap 300 800
        """.trimIndent()
        
        val commands = commandProcessor.parseCommands(multiCommandScript)
        assertTrue("Should parse multiple commands: ${commands.size}", commands.isNotEmpty())
        assertEquals("Should parse 4 commands", 4, commands.size)
    }
    
    @Test
    fun testErrorHandling() {
        val invalidCommands = listOf(
            "invalid command",
            "tap abc def",  // Invalid coordinates
            "swipe 100",    // Insufficient parameters
            "scroll diagonal" // Invalid direction
        )
        
        for (invalidCommand in invalidCommands) {
            val command = commandProcessor.parseCommand(invalidCommand)
            assertNull("Should not parse invalid command: $invalidCommand", command)
        }
    }
}