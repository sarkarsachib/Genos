package com.example.screencapture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ScreenCaptureManagerTest {

    private lateinit var context: Context
    private lateinit var manager: ScreenCaptureManager
    private lateinit var mockOcrProcessor: OcrProcessor

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockOcrProcessor = mock()
        manager = ScreenCaptureManager(context, mockOcrProcessor)
    }

    @Test
    fun testRequestPermission() {
        val activity = mock<Activity>()
        manager.requestScreenCapturePermission(activity, 1001)
        verify(activity).startActivityForResult(any(), org.mockito.kotlin.eq(1001))
    }

    @Test
    fun testStartSession() {
        val intent = Intent()
        // We expect no exception
        manager.startSession(Activity.RESULT_OK, intent, 1080, 1920, 480)
    }

    @Test
    fun testCaptureOnceFailsWithoutSession() {
        val latch = CountDownLatch(1)
        manager.captureOnce { result ->
            Assert.assertTrue("Should fail without session", result.isFailure)
            latch.countDown()
        }
        latch.await(1, TimeUnit.SECONDS)
    }
}
