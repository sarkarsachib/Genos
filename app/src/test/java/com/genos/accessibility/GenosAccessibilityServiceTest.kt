package com.genos.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.genos.accessibility.model.UiElement
import com.genos.accessibility.model.UiTree
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GenosAccessibilityServiceTest {

    private lateinit var serviceController: ServiceController<GenosAccessibilityService>
    private lateinit var service: GenosAccessibilityService

    @Before
    fun setup() {
        serviceController = Robolectric.buildService(GenosAccessibilityService::class.java)
        service = serviceController.get()
    }

    @Test
    fun `test service onCreate sets running state to true`() = runTest {
        // When
        serviceController.create()
        advanceUntilIdle()
        
        // Then
        assertTrue(service.isServiceRunning.value)
    }

    @Test
    fun `test service onServiceConnected configures service info`() {
        // When
        serviceController.create()
        service.onServiceConnected()
        
        // Then
        assertNotNull(service.serviceInfo)
        assertTrue(service.isServiceRunning.value)
    }

    @Test
    fun `test service processes accessibility event`() {
        // Given
        serviceController.create()
        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        every { event.packageName } returns "com.test.app"
        
        // When
        service.onAccessibilityEvent(event)
        
        // Then
        verify { event.eventType }
        verify { event.packageName }
    }

    @Test
    fun `test service handles null package name`() {
        // Given
        serviceController.create()
        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        every { event.packageName } returns null
        
        // When
        service.onAccessibilityEvent(event)
        
        // Then - should not crash
        assertTrue(true)
    }

    @Test
    fun `test service onInterrupt`() {
        // When
        serviceController.create()
        service.onInterrupt()
        
        // Then - should not crash
        assertTrue(true)
    }

    @Test
    fun `test service onUnbind cleans up`() = runTest {
        // Given
        serviceController.create()
        val intent = mockk<Intent>(relaxed = true)
        
        // When
        service.onUnbind(intent)
        advanceUntilIdle()
        
        // Then
        assertFalse(service.isServiceRunning.value)
    }

    @Test
    fun `test service onDestroy cleans up`() = runTest {
        // Given
        serviceController.create()
        
        // When
        serviceController.destroy()
        advanceUntilIdle()
        
        // Then
        assertFalse(service.isServiceRunning.value)
    }

    @Test
    fun `test getCurrentUiTree returns null initially`() {
        // Given
        serviceController.create()
        
        // When
        val tree = service.getCurrentUiTree()
        
        // Then
        assertNull(tree)
    }

    @Test
    fun `test getCurrentAppPackage returns null initially`() {
        // Given
        serviceController.create()
        
        // When
        val packageName = service.getCurrentAppPackage()
        
        // Then
        assertNull(packageName)
    }

    @Test
    fun `test service handles window content changed event`() {
        // Given
        serviceController.create()
        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        every { event.packageName } returns "com.test.app"
        
        // When
        service.onAccessibilityEvent(event)
        
        // Then
        verify { event.eventType }
    }

    @Test
    fun `test service handles exception in accessibility event`() {
        // Given
        serviceController.create()
        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } throws RuntimeException("Test exception")
        
        // When & Then - should not crash
        service.onAccessibilityEvent(event)
        assertTrue(true)
    }

    @Test
    fun `test latestUiTree flow is initially null`() = runTest {
        // Given
        serviceController.create()
        
        // When
        val latestTree = service.latestUiTree.value
        
        // Then
        assertNull(latestTree)
    }

    @Test
    fun `test isServiceRunning flow updates correctly`() = runTest {
        // Given & When
        serviceController.create()
        advanceUntilIdle()
        val runningAfterCreate = service.isServiceRunning.value
        
        serviceController.destroy()
        advanceUntilIdle()
        val runningAfterDestroy = service.isServiceRunning.value
        
        // Then
        assertTrue(runningAfterCreate)
        assertFalse(runningAfterDestroy)
    }
}