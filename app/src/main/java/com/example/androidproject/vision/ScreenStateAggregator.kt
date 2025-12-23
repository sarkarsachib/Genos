package com.example.androidproject.vision

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.androidproject.accessibility.AccessibilityTreeNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenStateAggregator(private val context: Context) {
    companion object {
        private const val TAG = "ScreenStateAggregator"
        private const val SCREENSHOT_DIR = "ScreenCaptures"
    }

    /**
     * Aggregate screen state combining screenshot, OCR results, and accessibility tree
     */
    suspend fun aggregateScreenState(
        bitmap: Bitmap,
        ocrResult: OcrResult,
        accessibilityTree: List<AccessibilityTreeNode>
    ): ScreenStateResult {
        return withContext(Dispatchers.IO) {
            try {
                // Save screenshot to persistent storage
                val screenshotUri = saveScreenshotToStorage(bitmap)
                
                // Extract OCR text and bounding boxes
                val ocrText = extractOcrText(ocrResult)
                val ocrBoundingBoxes = extractOcrBoundingBoxes(ocrResult)
                
                // Process accessibility tree
                val uiElements = processAccessibilityTree(accessibilityTree)
                
                // Create aggregated payload
                val screenState = ScreenState(
                    screenshotUri = screenshotUri,
                    timestamp = System.currentTimeMillis(),
                    ocrText = ocrText,
                    ocrBoundingBoxes = ocrBoundingBoxes,
                    uiElements = uiElements,
                    metadata = createMetadata(bitmap, ocrResult, accessibilityTree)
                )
                
                Log.d(TAG, "Successfully aggregated screen state")
                ScreenStateResult.Success(screenState)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error aggregating screen state", e)
                ScreenStateResult.Error("Failed to aggregate screen state: ${e.message}")
            }
        }
    }

    /**
     * Aggregate screen state without accessibility tree (fallback)
     */
    suspend fun aggregateScreenStateSimple(
        bitmap: Bitmap,
        ocrResult: OcrResult
    ): ScreenStateResult {
        return withContext(Dispatchers.IO) {
            try {
                val screenshotUri = saveScreenshotToStorage(bitmap)
                val ocrText = extractOcrText(ocrResult)
                val ocrBoundingBoxes = extractOcrBoundingBoxes(ocrResult)
                
                val screenState = ScreenState(
                    screenshotUri = screenshotUri,
                    timestamp = System.currentTimeMillis(),
                    ocrText = ocrText,
                    ocrBoundingBoxes = ocrBoundingBoxes,
                    uiElements = emptyList(),
                    metadata = createMetadata(bitmap, ocrResult, emptyList())
                )
                
                ScreenStateResult.Success(screenState)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error aggregating simple screen state", e)
                ScreenStateResult.Error("Failed to aggregate screen state: ${e.message}")
            }
        }
    }

    /**
     * Save screenshot to external storage
     */
    private suspend fun saveScreenshotToStorage(bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "screenshot_$timestamp.jpg"
        
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            SCREENSHOT_DIR
        )
        
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        val file = File(directory, filename)
        
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.flush()
        }
        
        Log.d(TAG, "Screenshot saved to: ${file.absolutePath}")
        Uri.fromFile(file)
    }

    /**
     * Extract plain text from OCR result
     */
    private fun extractOcrText(ocrResult: OcrResult): String {
        return when (ocrResult) {
            is OcrResult.Success -> {
                ocrResult.textBlocks.joinToString(" ") { block ->
                    block.lines.joinToString(" ") { line ->
                        line.text
                    }
                }
            }
            is OcrResult.Error -> ""
        }
    }

    /**
     * Extract bounding boxes from OCR result
     */
    private fun extractOcrBoundingBoxes(ocrResult: OcrResult): List<TextBoundingBox> {
        return when (ocrResult) {
            is OcrResult.Success -> {
                ocrResult.textBlocks.flatMap { block ->
                    block.lines.flatMap { line ->
                        line.elements.map { element ->
                            TextBoundingBox(
                                text = element.text,
                                boundingBox = element.boundingBox,
                                confidence = element.confidence
                            )
                        }
                    }
                }
            }
            is OcrResult.Error -> emptyList()
        }
    }

    /**
     * Process accessibility tree into UI elements
     */
    private fun processAccessibilityTree(tree: List<AccessibilityTreeNode>): List<UiElement> {
        return tree.map { node ->
            UiElement(
                className = node.className,
                text = node.text,
                contentDescription = node.contentDescription,
                bounds = node.bounds,
                isClickable = node.isClickable,
                isFocusable = node.isFocusable,
                isEnabled = node.isEnabled,
                isVisible = node.isVisible,
                resourceId = node.resourceId,
                packageName = node.packageName,
                viewHierarchy = getViewHierarchy(node)
            )
        }
    }

    /**
     * Get view hierarchy path for an element
     */
    private fun getViewHierarchy(node: AccessibilityTreeNode): String {
        val path = mutableListOf<String>()
        var current: AccessibilityTreeNode? = node
        
        while (current != null) {
            val name = current.className ?: current.resourceId ?: "Unknown"
            path.add(0, name)
            current = current.parent
        }
        
        return path.joinToString(" > ")
    }

    /**
     * Create metadata for the screen state
     */
    private fun createMetadata(
        bitmap: Bitmap,
        ocrResult: OcrResult,
        accessibilityTree: List<AccessibilityTreeNode>
    ): ScreenMetadata {
        return ScreenMetadata(
            bitmapWidth = bitmap.width,
            bitmapHeight = bitmap.height,
            ocrStatus = when (ocrResult) {
                is OcrResult.Success -> "success"
                is OcrResult.Error -> "error: ${ocrResult.message}"
            },
            ocrTextBlocksCount = when (ocrResult) {
                is OcrResult.Success -> ocrResult.textBlocks.size
                is OcrResult.Error -> 0
            },
            accessibilityNodeCount = accessibilityTree.size,
            hasAccessibilityData = accessibilityTree.isNotEmpty(),
            pixelDensity = context.resources.displayMetrics.density
        )
    }

    /**
     * Clean up old screenshots to manage storage
     */
    suspend fun cleanupOldScreenshots(maxAgeDays: Int = 7) = withContext(Dispatchers.IO) {
        try {
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                SCREENSHOT_DIR
            )
            
            if (!directory.exists()) return@withContext
            
            val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000)
            val files = directory.listFiles() ?: return@withContext
            
            files.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                    Log.d(TAG, "Deleted old screenshot: ${file.name}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old screenshots", e)
        }
    }
}

/**
 * Data classes for screen state aggregation
 */
data class ScreenState(
    val screenshotUri: Uri,
    val timestamp: Long,
    val ocrText: String,
    val ocrBoundingBoxes: List<TextBoundingBox>,
    val uiElements: List<UiElement>,
    val metadata: ScreenMetadata
)

data class TextBoundingBox(
    val text: String,
    val boundingBox: android.graphics.Rect,
    val confidence: Float
)

data class UiElement(
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val bounds: android.graphics.Rect,
    val isClickable: Boolean,
    val isFocusable: Boolean,
    val isEnabled: Boolean,
    val isVisible: Boolean,
    val resourceId: String?,
    val packageName: String?,
    val viewHierarchy: String
)

data class ScreenMetadata(
    val bitmapWidth: Int,
    val bitmapHeight: Int,
    val ocrStatus: String,
    val ocrTextBlocksCount: Int,
    val accessibilityNodeCount: Int,
    val hasAccessibilityData: Boolean,
    val pixelDensity: Float
)

sealed class ScreenStateResult {
    data class Success(val screenState: ScreenState) : ScreenStateResult()
    data class Error(val message: String) : ScreenStateResult()
}