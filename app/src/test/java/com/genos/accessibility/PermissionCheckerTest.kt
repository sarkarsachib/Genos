package com.genos.accessibility

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PermissionCheckerTest {

    private lateinit var mockContext: Context
    private lateinit var mockContentResolver: ContentResolver

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockContentResolver = mockk(relaxed = true)
        every { mockContext.contentResolver } returns mockContentResolver
        mockkStatic(Settings::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `test isAccessibilityServiceEnabled returns true when service is enabled with short name`() {
        // Given
        val enabledServices = "com.genos.accessibility/.GenosAccessibilityService:com.other/Service"
        every { 
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices
        
        // When
        val result = PermissionChecker.isAccessibilityServiceEnabled(mockContext)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `test isAccessibilityServiceEnabled returns true when service is enabled with full name`() {
        // Given
        val enabledServices = "com.genos.accessibility/com.genos.accessibility.GenosAccessibilityService"
        every { 
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices
        
        // When
        val result = PermissionChecker.isAccessibilityServiceEnabled(mockContext)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `test isAccessibilityServiceEnabled returns false when service is not enabled`() {
        // Given
        val enabledServices = "com.other.app/.SomeService:com.another/.AnotherService"
        every { 
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices
        
        // When
        val result = PermissionChecker.isAccessibilityServiceEnabled(mockContext)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `test isAccessibilityServiceEnabled returns false when settings is null`() {
        // Given
        every { 
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns null
        
        // When
        val result = PermissionChecker.isAccessibilityServiceEnabled(mockContext)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `test isAccessibilityServiceEnabled returns false when settings is empty`() {
        // Given
        every { 
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns ""
        
        // When
        val result = PermissionChecker.isAccessibilityServiceEnabled(mockContext)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `test isAccessibilityServiceEnabled handles exception gracefully`() {
        // Given
        every { 
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } throws SecurityException("Access denied")
        
        // When
        val result = PermissionChecker.isAccessibilityServiceEnabled(mockContext)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `test isAccessibilityServiceEnabled with whitespace in settings`() {
        // Given
        val enabledServices = "  com.genos.accessibility/.GenosAccessibilityService  :  com.other/Service  "
        every { 
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices
        
        // When
        val result = PermissionChecker.isAccessibilityServiceEnabled(mockContext)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `test isAccessibilityServiceEnabled with only target service`() {
        // Given
        val enabledServices = "com.genos.accessibility/.GenosAccessibilityService"
        every { 
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices
        
        // When
        val result = PermissionChecker.isAccessibilityServiceEnabled(mockContext)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `test canDrawOverlays returns true when permission granted`() {
        // Given
        every { Settings.canDrawOverlays(mockContext) } returns true
        
        // When
        val result = PermissionChecker.canDrawOverlays(mockContext)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `test canDrawOverlays returns false when permission denied`() {
        // Given
        every { Settings.canDrawOverlays(mockContext) } returns false
        
        // When
        val result = PermissionChecker.canDrawOverlays(mockContext)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `test canDrawOverlays handles exception gracefully`() {
        // Given
        every { Settings.canDrawOverlays(mockContext) } throws SecurityException("Access denied")
        
        // When
        val result = PermissionChecker.canDrawOverlays(mockContext)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `test isAccessibilityServiceEnabled with case sensitivity`() {
        // Given
        val enabledServices = "com.genos.accessibility/.GenosAccessibilityService"
        every { 
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices
        
        // When
        val result = PermissionChecker.isAccessibilityServiceEnabled(mockContext)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `test isAccessibilityServiceEnabled with similar but different package name`() {
        // Given
        val enabledServices = "com.genos.accessibility.other/.Service"
        every { 
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices
        
        // When
        val result = PermissionChecker.isAccessibilityServiceEnabled(mockContext)
        
        // Then
        assertFalse(result)
    }
}