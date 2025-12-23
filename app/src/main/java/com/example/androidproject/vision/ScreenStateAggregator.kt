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
     * Aggregate screenshot, OCR results, and accessibility nodes into a single ScreenState payload.
     *
     * Collects a stored screenshot URI, merged OCR text and bounding boxes, processed UI elements from
     * the accessibility tree, and accompanying metadata into a ScreenState result.
     *
     * @param bitmap The captured screen image to save and include in the aggregated state.
     * @param ocrResult The OCR extraction result used to produce aggregated text and bounding boxes.
     * @param accessibilityTree The list of accessibility nodes to convert into UI element representations.
     * @return `ScreenStateResult.Success` containing the assembled ScreenState on success, `ScreenStateResult.Error` with a descriptive message on failure.
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
     * Aggregate a ScreenState using the provided screenshot and OCR results, omitting accessibility data.
     *
     * Saves the bitmap to storage, extracts OCR text and bounding boxes, builds a ScreenState with an empty
     * UI elements list and generated metadata, and returns the aggregation result.
     *
     * @param bitmap The screenshot bitmap to save and include in the ScreenState.
     * @param ocrResult The OCR result used to extract aggregated text and bounding boxes.
     * @return `ScreenStateResult.Success` with the constructed ScreenState on success, `ScreenStateResult.Error` with an error message on failure.
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
     * Saves a bitmap as a timestamped JPEG file in the Pictures/ScreenCaptures directory.
     *
     * Creates the target directory if it does not exist and returns a Uri pointing to the saved file.
     *
     * @param bitmap The image to write to storage.
     * @return A Uri referencing the saved JPEG file.
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
     * Concatenates recognized text from an OCR result into a single plain string.
     *
     * @param ocrResult The OCR result containing text blocks or an error.
     * @return The concatenated text from all blocks and lines when OCR succeeded, or an empty string if OCR failed.
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
     * Produce a list of TextBoundingBox objects extracted from a successful OCR result.
     *
     * If the OCR result represents an error, returns an empty list.
     *
     * @param ocrResult The OCR result to extract bounding boxes from.
     * @return A list of `TextBoundingBox` containing text, bounding box, and confidence for each OCR element; empty if OCR failed.
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
     * Convert an accessibility node list into a list of UiElement objects.
     *
     * @param tree The accessibility tree nodes to transform.
     * @return A list of `UiElement` representing each node, including computed `viewHierarchy` for each element.
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
     * Builds a hierarchical view path for the given accessibility node.
     *
     * Each ancestor's className is used when available, otherwise resourceId is used;
     * if both are null the segment is "Unknown". Ancestor segments are joined with " > ".
     *
     * @param node The accessibility tree node to generate the hierarchy for.
     * @return A string representing the node's ancestor path (root to node) joined by " > ".
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
     * Builds a ScreenMetadata object summarizing the provided bitmap, OCR result, and accessibility tree.
     *
     * @param bitmap Source bitmap used to record image width and height.
     * @param ocrResult OCR processing result used to derive `ocrStatus` and `ocrTextBlocksCount`.
     * @param accessibilityTree Accessibility nodes list used to derive `accessibilityNodeCount` and `hasAccessibilityData`.
     * @return A ScreenMetadata containing bitmap dimensions, OCR status and text-block count, accessibility node count and presence flag, and device pixel density.
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
     * Deletes screenshot files older than the specified number of days from the application's
     * "ScreenCaptures" directory in the device Pictures folder.
     *
     * @param maxAgeDays Number of days; files with last-modified timestamps older than this
     * value will be removed. Defaults to 7.
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