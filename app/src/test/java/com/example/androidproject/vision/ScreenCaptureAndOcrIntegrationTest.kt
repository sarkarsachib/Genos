package com.example.androidproject.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenCaptureAndOcrIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var ocrProcessor: OcrProcessor
    private lateinit var screenStateAggregator: ScreenStateAggregator

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        ocrProcessor = OcrProcessor()
        screenStateAggregator = ScreenStateAggregator(context)
    }

    @Test
    fun testOcrProcessorInitialization() {
        assertNotNull("OCR Processor should be initialized", ocrProcessor)
    }

    @Test
    fun testScreenStateAggregatorInitialization() {
        assertNotNull("Screen State Aggregator should be initialized", screenStateAggregator)
    }

    @Test
    fun testTextBlockCreation() {
        val testRect = Rect(10, 10, 100, 50)
        val testText = "Test Text Block"
        
        val textBlock = TextBlock(
            text = testText,
            boundingBox = testRect,
            lines = emptyList()
        )
        
        assertEquals("Text should match", testText, textBlock.text)
        assertEquals("Bounding box should match", testRect, textBlock.boundingBox)
        assertTrue("Should have empty lines initially", textBlock.lines.isEmpty())
    }

    @Test
    fun testTextElementCreation() {
        val testRect = Rect(20, 20, 80, 40)
        val testText = "Element"
        val testConfidence = 0.85f
        
        val textElement = TextElement(
            text = testText,
            boundingBox = testRect,
            confidence = testConfidence
        )
        
        assertEquals("Text should match", testText, textElement.text)
        assertEquals("Bounding box should match", testRect, textElement.boundingBox)
        assertEquals("Confidence should match", testConfidence, textElement.confidence, 0.01f)
    }

    @Test
    fun testScreenCaptureResultTypes() {
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        
        // Test Success case
        val successResult = ScreenCaptureResult.Success(testBitmap)
        assertTrue("Should be success type", successResult is ScreenCaptureResult.Success)
        if (successResult is ScreenCaptureResult.Success) {
            assertNotNull("Bitmap should not be null", successResult.bitmap)
            assertEquals("Bitmap dimensions should match", 100, successResult.bitmap.width)
            assertEquals("Bitmap dimensions should match", 100, successResult.bitmap.height)
        }
        
        // Test Error case
        val errorMessage = "Test error message"
        val errorResult = ScreenCaptureResult.Error(errorMessage)
        assertTrue("Should be error type", errorResult is ScreenCaptureResult.Error)
        if (errorResult is ScreenCaptureResult.Error) {
            assertEquals("Error message should match", errorMessage, errorResult.message)
        }
    }

    @Test
    fun testOcrResultTypes() {
        val testBlocks = listOf(
            TextBlock(
                text = "Test block",
                boundingBox = Rect(0, 0, 50, 20),
                lines = emptyList()
            )
        )
        
        // Test Success case
        val successResult = OcrResult.Success(testBlocks)
        assertTrue("Should be success type", successResult is OcrResult.Success)
        if (successResult is OcrResult.Success) {
            assertEquals("Should have correct number of blocks", 1, successResult.textBlocks.size)
            assertEquals("First block text should match", "Test block", successResult.textBlocks[0].text)
        }
        
        // Test Error case
        val errorMessage = "No text found"
        val errorResult = OcrResult.Error(errorMessage)
        assertTrue("Should be error type", errorResult is OcrResult.Error)
        if (errorResult is OcrResult.Error) {
            assertEquals("Error message should match", errorMessage, errorResult.message)
        }
    }

    @Test
    fun testUiElementCreation() {
        val testBounds = Rect(10, 10, 100, 50)
        
        val uiElement = UiElement(
            className = "Button",
            text = "Click Me",
            contentDescription = "Click button",
            bounds = testBounds,
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            isVisible = true,
            resourceId = "btn_click",
            packageName = "com.example.test",
            viewHierarchy = "LinearLayout > Button"
        )
        
        assertEquals("Class name should match", "Button", uiElement.className)
        assertEquals("Text should match", "Click Me", uiElement.text)
        assertEquals("Bounds should match", testBounds, uiElement.bounds)
        assertTrue("Should be clickable", uiElement.isClickable)
        assertTrue("Should be focusable", uiElement.isFocusable)
        assertTrue("Should be enabled", uiElement.isEnabled)
        assertTrue("Should be visible", uiElement.isVisible)
        assertEquals("Resource ID should match", "btn_click", uiElement.resourceId)
        assertEquals("Package name should match", "com.example.test", uiElement.packageName)
        assertEquals("View hierarchy should match", "LinearLayout > Button", uiElement.viewHierarchy)
    }

    @Test
    fun testScreenStateAggregationSimple() = runBlocking {
        val testBitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val ocrResult = OcrResult.Success(emptyList())
        
        val result = screenStateAggregator.aggregateScreenStateSimple(testBitmap, ocrResult)
        
        when (result) {
            is ScreenStateResult.Success -> {
                assertNotNull("Screenshot URI should be created", result.screenState.screenshotUri)
                assertTrue("Timestamp should be recent", result.screenState.timestamp > 0)
                assertTrue("OCR text should be empty for test", result.screenState.ocrText.isEmpty())
            }
            is ScreenStateResult.Error -> {
                fail("Screen state aggregation should not fail: ${result.message}")
            }
        }
    }
}