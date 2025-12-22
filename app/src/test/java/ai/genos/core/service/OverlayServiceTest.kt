package ai.genos.core.service

import android.app.Service
import android.content.Intent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OverlayServiceTest {

    private lateinit var service: OverlayService
    private lateinit var controller: ServiceController<OverlayService>

    @Before
    fun setUp() {
        controller = Robolectric.buildService(OverlayService::class.java)
        service = controller.create().get()
    }

    @Test
    fun `service is instance of Service`() {
        assertTrue(service is Service)
    }

    @Test
    fun `onCreate does not throw exception`() {
        try {
            service.onCreate()
            assertTrue(true)
        } catch (e: Exception) {
            fail("onCreate should not throw exception: ${e.message}")
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
    fun `onStartCommand with different flags`() {
        val intent = Intent()
        val result1 = service.onStartCommand(intent, 0, 1)
        val result2 = service.onStartCommand(intent, Service.START_FLAG_REDELIVERY, 1)
        val result3 = service.onStartCommand(intent, Service.START_FLAG_RETRY, 1)
        
        assertEquals(Service.START_STICKY, result1)
        assertEquals(Service.START_STICKY, result2)
        assertEquals(Service.START_STICKY, result3)
    }

    @Test
    fun `onBind can be called multiple times`() {
        val intent = Intent()
        val binder1 = service.onBind(intent)
        val binder2 = service.onBind(intent)
        
        assertNull(binder1)
        assertNull(binder2)
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
    fun `multiple onCreate calls are safe`() {
        try {
            service.onCreate()
            service.onCreate()
            assertTrue(true)
        } catch (e: Exception) {
            fail("Multiple onCreate calls should be safe: ${e.message}")
        }
    }

    @Test
    fun `multiple onDestroy calls are safe`() {
        try {
            service.onDestroy()
            service.onDestroy()
            assertTrue(true)
        } catch (e: Exception) {
            fail("Multiple onDestroy calls should be safe: ${e.message}")
        }
    }
}