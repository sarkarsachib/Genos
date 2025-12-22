package com.genos.accessibility

import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PermissionActivityTest {

    @Before
    fun setup() {
        mockkObject(PermissionChecker)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun testActivityLaunches() {
        // Given
        every { PermissionChecker.isAccessibilityServiceEnabled(any()) } returns false
        every { PermissionChecker.canDrawOverlays(any()) } returns false
        
        // When
        val scenario = ActivityScenario.launch(PermissionActivity::class.java)
        
        // Then
        onView(withId(R.id.btn_enable_accessibility)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_enable_overlay)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_start_service)).check(matches(isDisplayed()))
        
        scenario.close()
    }

    @Test
    fun testStartServiceButtonDisabledWhenPermissionsNotGranted() {
        // Given
        every { PermissionChecker.isAccessibilityServiceEnabled(any()) } returns false
        every { PermissionChecker.canDrawOverlays(any()) } returns false
        
        // When
        val scenario = ActivityScenario.launch(PermissionActivity::class.java)
        
        // Then
        onView(withId(R.id.btn_start_service)).check(matches(isNotEnabled()))
        
        scenario.close()
    }

    @Test
    fun testStartServiceButtonEnabledWhenAllPermissionsGranted() {
        // Given
        every { PermissionChecker.isAccessibilityServiceEnabled(any()) } returns true
        every { PermissionChecker.canDrawOverlays(any()) } returns true
        
        // When
        val scenario = ActivityScenario.launch(PermissionActivity::class.java)
        
        // Then
        onView(withId(R.id.btn_start_service)).check(matches(isEnabled()))
        
        scenario.close()
    }

    @Test
    fun testAccessibilityButtonTextChangesBasedOnPermission() {
        // Test when permission not granted
        every { PermissionChecker.isAccessibilityServiceEnabled(any()) } returns false
        every { PermissionChecker.canDrawOverlays(any()) } returns false
        
        var scenario = ActivityScenario.launch(PermissionActivity::class.java)
        onView(withId(R.id.btn_enable_accessibility)).check(matches(withText("Enable")))
        scenario.close()
        
        // Test when permission granted
        every { PermissionChecker.isAccessibilityServiceEnabled(any()) } returns true
        every { PermissionChecker.canDrawOverlays(any()) } returns false
        
        scenario = ActivityScenario.launch(PermissionActivity::class.java)
        onView(withId(R.id.btn_enable_accessibility)).check(matches(withText("Disable")))
        scenario.close()
    }

    @Test
    fun testOverlayButtonTextChangesBasedOnPermission() {
        // Test when permission not granted
        every { PermissionChecker.isAccessibilityServiceEnabled(any()) } returns false
        every { PermissionChecker.canDrawOverlays(any()) } returns false
        
        var scenario = ActivityScenario.launch(PermissionActivity::class.java)
        onView(withId(R.id.btn_enable_overlay)).check(matches(withText("Enable")))
        scenario.close()
        
        // Test when permission granted
        every { PermissionChecker.isAccessibilityServiceEnabled(any()) } returns false
        every { PermissionChecker.canDrawOverlays(any()) } returns true
        
        scenario = ActivityScenario.launch(PermissionActivity::class.java)
        onView(withId(R.id.btn_enable_overlay)).check(matches(withText("Disable")))
        scenario.close()
    }

    @Test
    fun testLogsTextViewIsDisplayed() {
        // Given
        every { PermissionChecker.isAccessibilityServiceEnabled(any()) } returns false
        every { PermissionChecker.canDrawOverlays(any()) } returns false
        
        // When
        val scenario = ActivityScenario.launch(PermissionActivity::class.java)
        
        // Then
        onView(withId(R.id.tv_logs)).check(matches(isDisplayed()))
        
        scenario.close()
    }
}