package ai.genos.core

import android.app.Application
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GenosApplicationTest {

    private lateinit var application: GenosApplication

    @Before
    fun setUp() {
        application = Robolectric.setupApplication(GenosApplication::class.java)
    }

    @Test
    fun `application instance is set after onCreate`() {
        assertNotNull(GenosApplication.instance)
        assertEquals(application, GenosApplication.instance)
    }

    @Test
    fun `application is instance of Application`() {
        assertTrue(application is Application)
    }

    @Test
    fun `companion instance is accessible`() {
        val instance = GenosApplication.instance
        assertNotNull(instance)
        assertTrue(instance is GenosApplication)
    }

    @Test
    fun `application context is not null`() {
        assertNotNull(application.applicationContext)
    }

    @Test
    fun `application has correct package name`() {
        assertEquals("ai.genos.core", application.packageName)
    }
}