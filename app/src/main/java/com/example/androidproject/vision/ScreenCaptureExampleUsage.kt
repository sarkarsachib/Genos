package com.example.androidproject.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.*

/**
 * Comprehensive usage example demonstrating the complete screen capture and OCR pipeline
 * Shows both ML Kit and Tesseract OCR options
 */
class ScreenCaptureExampleUsage(private val context: Context) {
    
    companion object {
        private const val TAG = "ScreenCaptureExample"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isUsingTesseract = false

    /**
     * Example 1: Basic ML Kit OCR Pipeline
     */
    fun exampleMlKitOcrPipeline() {
        scope.launch {
            try {
                // 1. Initialize components
                val ocrProcessor = OcrProcessor()
                val screenStateAggregator = ScreenStateAggregator(context)
                
                // 2. Simulate screen capture (in real implementation, this comes from ScreenCaptureManager)
                val capturedBitmap = simulateScreenCapture()
                
                // 3. Process OCR
                val ocrResult = ocrProcessor.processImage(capturedBitmap)
                
                // 4. Aggregate results
                val aggregatedResult = when (ocrResult) {
                    is OcrResult.Success -> {
                        screenStateAggregator.aggregateScreenStateSimple(capturedBitmap, ocrResult)
                    }
                    is OcrResult.Error -> {
                        ScreenStateResult.Error("OCR failed: ${ocrResult.message}")
                    }
                }
                
                // 5. Handle results
                handleAggregatedResult(aggregatedResult)
                
                // 6. Cleanup
                ocrProcessor.close()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in ML Kit pipeline", e)
            }
        }
    }

    /**
     * Example 2: Tesseract OCR Pipeline
     */
    fun exampleTesseractOcrPipeline() {
        scope.launch {
            try {
                // 1. Initialize Tesseract OCR
                val tesseractOcr = TesseractOcrProcessor(context)
                val initialized = tesseractOcr.initialize()
                
                if (!initialized) {
                    Log.e(TAG, "Failed to initialize Tesseract OCR")
                    return@launch
                }
                
                // 2. Configure Tesseract for better results
                tesseractOcr.setParameters(
                    pageSegMode = com.googlecode.tesseract.android.TessBaseAPI.PageSegMode.PSM_AUTO,
                    charWhitelist = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz.,!? "
                )
                
                // 3. Process captured screen
                val capturedBitmap = simulateScreenCapture()
                val tesseractResult = tesseractOcr.processImage(capturedBitmap)
                
                // 4. Handle Tesseract results
                when (tesseractResult) {
                    is TesseractOcrResult.Success -> {
                        Log.d(TAG, "Tesseract OCR Success:")
                        Log.d(TAG, "Text: ${tesseractResult.text}")
                        Log.d(TAG, "Confidence: ${tesseractResult.confidence}")
                        Log.d(TAG, "Text blocks found: ${tesseractResult.textBlocks.size}")
                        
                        // Convert to standard format for aggregation
                        val standardOcrResult = OcrResult.Success(tesseractResult.textBlocks)
                        val screenStateAggregator = ScreenStateAggregator(context)
                        val aggregatedResult = screenStateAggregator.aggregateScreenStateSimple(
                            capturedBitmap,
                            standardOcrResult
                        )
                        handleAggregatedResult(aggregatedResult)
                    }
                    is TesseractOcrResult.Error -> {
                        Log.e(TAG, "Tesseract OCR failed: ${tesseractResult.message}")
                    }
                }
                
                // 5. Cleanup
                tesseractOcr.close()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in Tesseract pipeline", e)
            }
        }
    }

    /**
     * Example 3: Regional OCR Processing
     */
    fun exampleRegionalOcrProcessing() {
        scope.launch {
            try {
                val ocrProcessor = OcrProcessor()
                
                val capturedBitmap = simulateScreenCapture()
                
                // Define regions of interest (e.g., status bar, content area, navigation)
                val regions = listOf(
                    android.graphics.Rect(0, 0, 1080, 100),  // Status bar region
                    android.graphics.Rect(0, 1500, 1080, 300) // Navigation region
                )
                
                val ocrResult = when (isUsingTesseract) {
                    true -> {
                        val tesseractOcr = TesseractOcrProcessor(context)
                        val initialized = tesseractOcr.initialize()
                        if (initialized) {
                            val result = tesseractOcr.processImageWithRegion(capturedBitmap, regions)
                            tesseractOcr.close()
                            convertTesseractResult(result)
                        } else {
                            OcrResult.Error("Tesseract initialization failed")
                        }
                    }
                    false -> {
                        ocrProcessor.processImageWithRegion(capturedBitmap, regions)
                    }
                }
                
                when (ocrResult) {
                    is OcrResult.Success -> {
                        Log.d(TAG, "Regional OCR found ${ocrResult.textBlocks.size} text blocks")
                        ocrResult.textBlocks.forEach { block ->
                            Log.d(TAG, "Text: ${block.text}, Position: ${block.boundingBox}")
                        }
                    }
                    is OcrResult.Error -> {
                        Log.e(TAG, "Regional OCR failed: ${ocrResult.message}")
                    }
                }
                
                ocrProcessor.close()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in regional OCR processing", e)
            }
        }
    }

    /**
     * Example 4: Complete Pipeline with Accessibility Integration
     */
    fun exampleCompletePipelineWithAccessibility() {
        scope.launch {
            try {
                val ocrProcessor = OcrProcessor()
                val screenStateAggregator = ScreenStateAggregator(context)
                
                val capturedBitmap = simulateScreenCapture()
                val ocrResult = ocrProcessor.processImage(capturedBitmap)
                
                // Simulate accessibility tree (in real implementation, this comes from accessibility service)
                val accessibilityTree = simulateAccessibilityTree()
                
                val aggregatedResult = when (ocrResult) {
                    is OcrResult.Success -> {
                        screenStateAggregator.aggregateScreenState(
                            capturedBitmap,
                            ocrResult,
                            accessibilityTree
                        )
                    }
                    is OcrResult.Error -> {
                        ScreenStateResult.Error("OCR failed: ${ocrResult.message}")
                    }
                }
                
                when (aggregatedResult) {
                    is ScreenStateResult.Success -> {
                        val screenState = aggregatedResult.screenState
                        Log.d(TAG, "Complete pipeline result:")
                        Log.d(TAG, "Screenshot URI: ${screenState.screenshotUri}")
                        Log.d(TAG, "OCR Text: ${screenState.ocrText}")
                        Log.d(TAG, "UI Elements: ${screenState.uiElements.size}")
                        Log.d(TAG, "Metadata: ${screenState.metadata}")
                        
                        // Prepare payload for transmission to Gemini
                        val transmissionPayload = createTransmissionPayload(screenState)
                        Log.d(TAG, "Payload ready for transmission: ${transmissionPayload.size} bytes")
                    }
                    is ScreenStateResult.Error -> {
                        Log.e(TAG, "Aggregation failed: ${aggregatedResult.message}")
                    }
                }
                
                ocrProcessor.close()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in complete pipeline", e)
            }
        }
    }

    /**
     * Example 5: Performance Comparison between OCR engines
     */
    fun exampleOcrPerformanceComparison() {
        scope.launch {
            try {
                val capturedBitmap = simulateScreenCapture()
                val results = mutableMapOf<String, Any>()
                
                // Test ML Kit OCR
                val mlKitStart = System.currentTimeMillis()
                val mlKitOcr = OcrProcessor()
                val mlKitResult = mlKitOcr.processImage(capturedBitmap)
                val mlKitEnd = System.currentTimeMillis()
                val mlKitTime = mlKitEnd - mlKitStart
                
                results["mlkit"] = mapOf(
                    "time_ms" to mlKitTime,
                    "success" to (mlKitResult is OcrResult.Success),
                    "text_blocks" to when (mlKitResult) {
                        is OcrResult.Success -> mlKitResult.textBlocks.size
                        is OcrResult.Error -> 0
                    }
                )
                
                mlKitOcr.close()
                
                // Test Tesseract OCR
                val tesseractStart = System.currentTimeMillis()
                val tesseractOcr = TesseractOcrProcessor(context)
                val tesseractInitialized = tesseractOcr.initialize()
                
                if (tesseractInitialized) {
                    val tesseractResult = tesseractOcr.processImage(capturedBitmap)
                    val tesseractEnd = System.currentTimeMillis()
                    val tesseractTime = tesseractEnd - tesseractStart
                    
                    results["tesseract"] = mapOf(
                        "time_ms" to tesseractTime,
                        "success" to (tesseractResult is TesseractOcrResult.Success),
                        "text_blocks" to when (tesseractResult) {
                            is TesseractOcrResult.Success -> tesseractResult.textBlocks.size
                            is TesseractOcrResult.Error -> 0
                        }
                    )
                    
                    tesseractOcr.close()
                } else {
                    results["tesseract"] = mapOf(
                        "time_ms" to 0,
                        "success" to false,
                        "error" to "Initialization failed"
                    )
                }
                
                Log.d(TAG, "OCR Performance Comparison:")
                Log.d(TAG, "ML Kit: ${results["mlkit"]}")
                Log.d(TAG, "Tesseract: ${results["tesseract"]}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in performance comparison", e)
            }
        }
    }

    private fun handleAggregatedResult(result: ScreenStateResult) {
        when (result) {
            is ScreenStateResult.Success -> {
                val screenState = result.screenState
                Log.d(TAG, "Aggregated screen state:")
                Log.d(TAG, "- Screenshot: ${screenState.screenshotUri}")
                Log.d(TAG, "- OCR Text length: ${screenState.ocrText.length}")
                Log.d(TAG, "- Text bounding boxes: ${screenState.ocrBoundingBoxes.size}")
                Log.d(TAG, "- UI elements: ${screenState.uiElements.size}")
            }
            is ScreenStateResult.Error -> {
                Log.e(TAG, "Aggregation error: ${result.message}")
            }
        }
    }

    private fun convertTesseractResult(result: TesseractOcrResult): OcrResult {
        return when (result) {
            is TesseractOcrResult.Success -> OcrResult.Success(result.textBlocks)
            is TesseractOcrResult.Error -> OcrResult.Error(result.message)
        }
    }

    private fun createTransmissionPayload(screenState: ScreenState): ByteArray {
        // Create JSON or binary payload for transmission to Gemini
        val payload = buildString {
            appendLine("{")
            appendLine("  \"timestamp\": ${screenState.timestamp},")
            appendLine("  \"screenshot_uri\": \"${screenState.screenshotUri}\",")
            appendLine("  \"ocr_text\": \"${screenState.ocrText}\",")
            appendLine("  \"ocr_boxes\": [")
            screenState.ocrBoundingBoxes.forEachIndexed { index, box ->
                appendLine("    {")
                appendLine("      \"text\": \"${box.text}\",")
                appendLine("      \"x\": ${box.boundingBox.left},")
                appendLine("      \"y\": ${box.boundingBox.top},")
                appendLine("      \"width\": ${box.boundingBox.width()},")
                appendLine("      \"height\": ${box.boundingBox.height()},")
                appendLine("      \"confidence\": ${box.confidence}")
                append("    }")
                if (index < screenState.ocrBoundingBoxes.size - 1) appendLine(",") else appendLine()
            }
            appendLine("  ],")
            appendLine("  \"ui_elements\": [")
            screenState.uiElements.forEachIndexed { index, element ->
                appendLine("    {")
                appendLine("      \"class\": \"${element.className}\",")
                appendLine("      \"text\": \"${element.text}\",")
                appendLine("      \"hierarchy\": \"${element.viewHierarchy}\"")
                append("    }")
                if (index < screenState.uiElements.size - 1) appendLine(",") else appendLine()
            }
            appendLine("  ]")
            append("}")
        }
        return payload.toByteArray()
    }

    private fun simulateScreenCapture(): Bitmap {
        // Simulate a captured screen bitmap
        return Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
    }

    private fun simulateAccessibilityTree(): List<com.example.androidproject.accessibility.AccessibilityTreeNode> {
        // Simulate accessibility tree nodes
        return listOf(
            com.example.androidproject.accessibility.AccessibilityTreeNode(
                className = "Button",
                text = "Submit",
                contentDescription = "Submit form",
                bounds = android.graphics.Rect(100, 1500, 400, 1600),
                isClickable = true,
                isFocusable = true,
                isEnabled = true,
                isVisible = true,
                resourceId = "btn_submit",
                packageName = "com.example.test"
            )
        )
    }

    fun cleanup() {
        scope.cancel()
    }
}