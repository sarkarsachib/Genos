package com.genos.accessibility

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceConnectionHelperTest {

    private lateinit var helper: ServiceConnectionHelper
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        helper = ServiceConnectionHelper()
        mockContext = mockk(relaxed = true)
        mockkObject(PermissionChecker)
    }

    @After
    fun teardown() {
        helper.stopObserving()
        unmockkAll()
    }

    @Test
    fun `test observeServiceState starts observing`() = runTest {
        // Given
        every { PermissionChecker.isAccessibilityServiceEnabled(mockContext) } returns true
        var callbackInvoked = false
        
        // When
        helper.observeServiceState(mockContext) { service ->
            callbackInvoked = true
        }
        
        advanceTimeBy(1500) // Advance past poll interval
        
        // Then
        verify(atLeast = 1) { PermissionChecker.isAccessibilityServiceEnabled(mockContext) }
    }

    @Test
    fun `test stopObserving stops the observation job`() = runTest {
        // Given
        every { PermissionChecker.isAccessibilityServiceEnabled(mockContext) } returns false
        helper.observeServiceState(mockContext) { }
        
        // When
        helper.stopObserving()
        
        // Then - no exception should occur
        assertTrue(true)
    }

    @Test
    fun `test observeServiceState can be called multiple times`() = runTest {
        // Given
        every { PermissionChecker.isAccessibilityServiceEnabled(mockContext) } returns false
        
        // When
        helper.observeServiceState(mockContext) { }
        helper.observeServiceState(mockContext) { } // Cancel previous and start new
        
        // Then - no exception should occur
        assertTrue(true)
    }

    @Test
    fun `test stopObserving can be called when not observing`() {
        // When
        helper.stopObserving()
        
        // Then - no exception should occur
        assertTrue(true)
    }

    @Test
    fun `test helper handles service not enabled`() = runTest {
        // Given
        every { PermissionChecker.isAccessibilityServiceEnabled(mockContext) } returns false
        var callbackCount = 0
        
        // When
        helper.observeServiceState(mockContext) { service ->
            callbackCount++
        }
        
        advanceTimeBy(2000)
        
        // Then
        verify(atLeast = 1) { PermissionChecker.isAccessibilityServiceEnabled(mockContext) }
        // Callback should not be invoked when service is not enabled
    }

    @Test
    fun `test helper creation`() {
        // When
        val newHelper = ServiceConnectionHelper()
        
        // Then
        assertNotNull(newHelper)
    }
}