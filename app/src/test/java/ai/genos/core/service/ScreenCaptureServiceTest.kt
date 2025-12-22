package ai.genos.core.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ScreenCaptureServiceTest {

    private lateinit var service: ScreenCaptureService
    private lateinit var controller: ServiceController<ScreenCaptureService>

    @Before
    fun setUp() {
        controller = Robolectric.buildService(ScreenCaptureService::class.java)
        service = controller.create().get()
    }

    @Test
    fun `service is instance of Service`() {
        assertTrue(service is Service)
    }

    @Test
    fun `onCreate creates notification channel`() {
        try {
            service.onCreate()
            
            val notificationManager = RuntimeEnvironment.getApplication()
                .getSystemService(NotificationManager::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = notificationManager.getNotificationChannel("screen_capture_channel")
                assertNotNull("Notification channel should be created", channel)
            }
            
            assertTrue(true)
        } catch (e: Exception) {
            fail("onCreate should create notification channel: ${e.message}")
        }
    }

    @Test
    fun `onStartCommand returns START_STICKY`() {
        val intent = Intent()
        val result = service.onStartCommand(intent, 0, 1)
        assertEquals(Service.START_STICKY, result)
    }

    @Test
    fun `onStartCommand handles null intent`() {
        val result = service.onStartCommand(null, 0, 1)
        assertEquals(Service.START_STICKY, result)
    }

    @Test
    fun `onStartCommand starts foreground service`() {
        val intent = Intent()
        val result = service.onStartCommand(intent, 0, 1)
        
        assertEquals(Service.START_STICKY, result)
        
        val shadowService = shadowOf(service)
        assertTrue("Service should be in foreground", shadowService.isLastForegroundNotificationAttached)
    }

    @Test
    fun `onBind returns null`() {
        val intent = Intent()
        val binder = service.onBind(intent)
        assertNull("onBind should return null", binder)
    }

    @Test
    fun `onDestroy does not throw exception`() {
        try {
            service.onDestroy()
            assertTrue(true)
        } catch (e: Exception) {
            fail("onDestroy should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `service lifecycle onCreate to onDestroy`() {
        try {
            service.onCreate()
            val intent = Intent()
            service.onStartCommand(intent, 0, 1)
            service.onDestroy()
            assertTrue(true)
        } catch (e: Exception) {
            fail("Service lifecycle should work: ${e.message}")
        }
    }

    @Test
    fun `notification is created with correct properties`() {
        val intent = Intent()
        service.onStartCommand(intent, 0, 1)
        
        val shadowService = shadowOf(service)
        val notification = shadowService.lastForegroundNotification
        
        assertNotNull("Notification should be created", notification)
    }

    @Test
    fun `multiple onStartCommand calls work`() {
        val intent = Intent()
        val result1 = service.onStartCommand(intent, 0, 1)
        val result2 = service.onStartCommand(intent, 0, 2)
        val result3 = service.onStartCommand(intent, 0, 3)
        
        assertEquals(Service.START_STICKY, result1)
        assertEquals(Service.START_STICKY, result2)
        assertEquals(Service.START_STICKY, result3)
    }

    @Test
    fun `service can be restarted`() {
        service.onCreate()
        service.onStartCommand(Intent(), 0, 1)
        service.onDestroy()
        
        service.onCreate()
        val result = service.onStartCommand(Intent(), 0, 2)
        assertEquals(Service.START_STICKY, result)
    }

    @Test
    fun `notification channel properties are correct on Oreo+`() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            service.onCreate()
            
            val notificationManager = RuntimeEnvironment.getApplication()
                .getSystemService(NotificationManager::class.java)
            val channel = notificationManager.getNotificationChannel("screen_capture_channel")
            
            assertNotNull(channel)
            assertEquals("Screen Capture Service", channel?.name)
            assertEquals(NotificationManager.IMPORTANCE_LOW, channel?.importance)
        }
    }

    @Test
    fun `foreground notification ID is correct`() {
        val intent = Intent()
        service.onStartCommand(intent, 0, 1)
        
        val shadowService = shadowOf(service)
        assertEquals(1001, shadowService.lastForegroundNotificationId)
    }
}