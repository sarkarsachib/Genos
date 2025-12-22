package ai.genos.core

import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityUnitTest {

    private lateinit var activity: MainActivity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
            .get()
    }

    @Test
    fun `activity is instance of ComponentActivity`() {
        assertTrue(activity is ComponentActivity)
    }

    @Test
    fun `activity onCreate does not throw exception`() {
        try {
            val activity = Robolectric.buildActivity(MainActivity::class.java)
                .create()
                .get()
            assertNotNull(activity)
        } catch (e: Exception) {
            fail("onCreate should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `activity has correct package name`() {
        assertEquals("ai.genos.core", activity.packageName)
    }

    @Test
    fun `activity is not finishing on create`() {
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `activity lifecycle works correctly`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
            .pause()
            .stop()
            .destroy()
            .get()
        assertNotNull(activity)
    }

    @Test
    fun `activity can be recreated`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
        
        controller.pause().stop().destroy()
        
        val newActivity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .get()
        
        assertNotNull(newActivity)
    }

    @Test
    fun `activity has valid context`() {
        assertNotNull(activity.applicationContext)
        assertNotNull(activity.baseContext)
    }

    @Test
    fun `activity savedInstanceState handling`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
        
        val bundle = controller.saveInstanceState()
        assertNotNull(bundle)
    }
}