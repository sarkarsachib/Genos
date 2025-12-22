package com.example.androidproject

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.example.androidproject.accessibility.MyAccessibilityService
import com.example.androidproject.debug.InputDebugHarnessActivity
import com.example.androidproject.input.model.InputCommand
import com.example.androidproject.input.model.InputResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for Input Emulation Engine.
 * Validates that the engine can execute commands without root and provides proper callbacks.
 */
@RunWith(AndroidJUnit4::class)
class InputEmulatorTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)
    
    private lateinit var device: UiDevice
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Wake up device
        device.wakeUp()
    }
    
    @Test
    fun testAccessibilityServiceIsAvailable() {
        // Verify accessibility service is declared in manifest
        val packageName = context.packageName
        val expectedService = "$packageName/.accessibility.MyAccessibilityService"
        
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        assertNotNull("Accessibility service should be configured", enabledServices)
        Log.d("InputEmulatorTest", "Enabled services: $enabledServices")
    }
    
    @Test
    fun testCommandModels() {
        // Test TapCommand
        val tapCommand = InputCommand.TapCommand(100, 200)
        assertEquals("Tap at ([Point(x=100, y=200)])", tapCommand.description())
        
        // Test SwipeCommand
        val swipeCommand = InputCommand.SwipeCommand(0, 0, 100, 100)
        assertTrue(swipeCommand.description().contains("Swipe"))
        
        // Test ScrollCommand
        val scrollCommand = InputCommand.ScrollCommand(0, -300)
        assertTrue(scrollCommand.description().contains("Scroll"))
        
        // Test TypeCommand
        val typeCommand = InputCommand.TypeCommand("Test input")
        assertTrue(typeCommand.description().contains("Type text"))
    }
    
    @Test
    fun testResultModels() {
        // Test Success
        val success = InputResult.Success("Test successful")
        assertTrue(success.isSuccess())
        assertFalse(success.isFailure())
        assertEquals("Test successful", success.description())
        
        // Test Failure
        val failure = InputResult.Failure("Test failed")
        assertTrue(failure.isFailure())
        assertFalse(failure.isSuccess())
        assertEquals("Test failed", failure.description())
    }
    
    @Test
    fun testInputExecutorInterface() {
        val mockExecutor = MockInputExecutor()
        
        var callbackInvoked = false
        var result: InputResult? = null
        
        mockExecutor.executeCommand(
            InputCommand.TapCommand(500, 800)
        ) { callbackResult ->
            callbackInvoked = true
            result = callbackResult
        }
        
        assertTrue("Callback should be invoked", callbackInvoked)
        assertTrue("Result should be success", result?.isSuccess() == true)
    }
    
    @Test
    fun testPointValidation() {
        val validPoint = com.example.androidproject.input.model.Point(100, 200)
        assertTrue(validPoint.isValid(1920, 1080))
        
        val invalidPoint = com.example.androidproject.input.model.Point(-100, 200)
        assertFalse(invalidPoint.isValid(1920, 1080))
        
        val outOfBoundsPoint = com.example.androidproject.input.model.Point(2000, 2000)
        assertFalse(outOfBoundsPoint.isValid(1920, 1080))
    }
    
    @Test
    fun testCoordinatesAcceptance() {
        // Test large screen coordinates
        val largeCoordTap = InputCommand.TapCommand(1500, 2500, 200L)
        assertTrue(largeCoordTap.description().contains("1500"))
        assertTrue(largeCoordTap.description().contains("2500"))
        
        // Test precise coordinate paths
        val multiPointTap = InputCommand.TapCommand(
            listOf(
                com.example.androidproject.input.model.Point(100, 100),
                com.example.androidproject.input.model.Point(200, 200),
                com.example.androidproject.input.model.Point(300, 300)
            ),
            150L
        )
        assertEquals(3, multiPointTap.points.size)
        assertEquals(150L, multiPointTap.durationMs)
    }
    
    @Test
    fun testVelocityConfiguration() {
        // Test swipe with custom velocity (via duration)
        val fastSwipe = InputCommand.SwipeCommand(0, 0, 1000, 1000, 100L) // Fast
        val slowSwipe = InputCommand.SwipeCommand(0, 0, 1000, 1000, 1000L) // Slow
        
        assertEquals(100L, fastSwipe.durationMs)
        assertEquals(1000L, slowSwipe.durationMs)
        assertTrue(fastSwipe.durationMs < slowSwipe.durationMs)
    }
    
    @Test
    fun testTextCommandAcceptance() {
        val shortText = InputCommand.TypeCommand("Hi")
        val longText = InputCommand.TypeCommand("This is a much longer text for testing")
        val specialText = InputCommand.TypeCommand("Text with numbers 123 and symbols !@#")
        
        assertEquals(2, shortText.text.length)
        assertTrue(longText.text.length > 20)
        assertTrue(specialText.text.contains("123"))
        
        // Test with IME commit
        val imeText = InputCommand.TypeCommand("Search query", commitIme = true)
        assertTrue(imeText.commitIme)
        
        // Test with clear existing
        val clearText = InputCommand.TypeCommand("New text", clearExisting = true)
        assertTrue(clearText.clearExisting)
    }
    
    @Test
    fun testDebugHarnessActivityLaunch() {
        // Test that debug harness can be launched
        val intent = Intent(context, InputDebugHarnessActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        assertDoesNotThrow {
            context.startActivity(intent)
        }
    }
    
    @Test
    fun testAccessibilityServiceCapabilities() {
        // Verify the service has gesture capabilities
        val serviceClass = MyAccessibilityService::class.java
        
        assertNotNull("Service should exist", serviceClass)
        
        // Check service has input execution method
        val executeMethod = serviceClass.getDeclaredMethod(
            "executeInputCommand", 
            InputCommand::class.java, 
            Function1::class.java
        )
        assertNotNull("Service should have executeInputCommand method", executeMethod)
        
        // Check service has ready check method
        val readyMethod = serviceClass.getDeclaredMethod("isInputReady")
        assertNotNull("Service should have isInputReady method", readyMethod)
    }
    
    // Mock implementation for testing
    private class MockInputExecutor : com.example.androidproject.input.InputExecutor {
        override fun executeCommand(command: InputCommand, callback: (InputResult) -> Unit) {
            // Simulate successful execution
            callback(InputResult.Success("Mock execution: ${command.description()}"))
        }
        
        override fun isReady(): Boolean = true
    }
    
    companion object {
        @JvmStatic
        fun Log.d(tag: String, message: String) {
            android.util.Log.d(tag, message)
        }
        
        @JvmStatic
        fun Log.e(tag: String, message: String, error: Throwable? = null) {
            android.util.Log.e(tag, message, error)
        }
        
        @JvmStatic
        fun Log.i(tag: String, message: String) {
            android.util.Log.i(tag, message)
        }
        
        @JvmStatic
        fun Log.w(tag: String, message: String) {
            android.util.Log.w(tag, message)
        }
    }
}