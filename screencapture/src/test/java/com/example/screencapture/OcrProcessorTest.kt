package com.example.screencapture

import android.graphics.Bitmap
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OcrProcessorTest {

    @Test
    fun testProcessorCreation() {
        val processor = OcrProcessor(mock())
        // Just verify it instantiates without crashing
    }
}
