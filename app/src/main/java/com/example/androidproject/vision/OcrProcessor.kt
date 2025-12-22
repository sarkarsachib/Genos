package com.example.androidproject.vision

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.vision.Frame
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrProcessor {
    companion object {
        private const val TAG = "OcrProcessor"
    }

    private val textRecognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    /**
     * Process bitmap and extract text with bounding boxes
     */
    suspend fun processImage(bitmap: Bitmap): OcrResult {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(inputImage)
            
            // Await the result using coroutine suspension
            val text = awaitTextResult(result)
            extractTextBlocks(text)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image for OCR", e)
            OcrResult.Error("OCR processing failed: ${e.message}")
        }
    }

    /**
     * Process bitmap with custom bounding box regions for focused OCR
     */
    suspend fun processImageWithRegion(bitmap: Bitmap, regions: List<Rect>): OcrResult {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            
            // Process each region separately
            val allTextBlocks = mutableListOf<TextBlock>()
            
            for (region in regions) {
                val croppedBitmap = Bitmap.createBitmap(
                    bitmap,
                    region.left,
                    region.top,
                    region.width(),
                    region.height()
                )
                
                val regionImage = InputImage.fromBitmap(croppedBitmap, 0)
                val result = textRecognizer.process(regionImage)
                val text = awaitTextResult(result)
                
                val blocks = extractTextBlocks(text)
                if (blocks is OcrResult.Success) {
                    // Adjust bounding boxes to original image coordinates
                    val adjustedBlocks = blocks.textBlocks.map { block ->
                        block.copy(
                            boundingBox = Rect(
                                block.boundingBox.left + region.left,
                                block.boundingBox.top + region.top,
                                block.boundingBox.right + region.left,
                                block.boundingBox.bottom + region.top
                            )
                        )
                    }
                    allTextBlocks.addAll(adjustedBlocks)
                }
            }
            
            if (allTextBlocks.isNotEmpty()) {
                OcrResult.Success(allTextBlocks)
            } else {
                OcrResult.Error("No text found in specified regions")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image regions for OCR", e)
            OcrResult.Error("Region OCR processing failed: ${e.message}")
        }
    }

    /**
     * Extract text blocks with bounding boxes from ML Kit result
     */
    private fun extractTextBlocks(textResult: Text): OcrResult {
        val textBlocks = mutableListOf<TextBlock>()
        
        try {
            for (block in textResult.textBlocks) {
                val blockText = block.text
                val blockBoundingBox = block.boundingBox
                
                if (blockBoundingBox != null) {
                    val lines = mutableListOf<TextLine>()
                    
                    for (line in block.lines) {
                        val lineText = line.text
                        val lineBoundingBox = line.boundingBox
                        
                        if (lineBoundingBox != null) {
                            val elements = mutableListOf<TextElement>()
                            
                            for (element in line.elements) {
                                val elementText = element.text
                                val elementBoundingBox = element.boundingBox
                                
                                if (elementBoundingBox != null) {
                                    elements.add(
                                        TextElement(
                                            text = elementText,
                                            boundingBox = elementBoundingBox,
                                            confidence = 0.0f // ML Kit doesn't provide confidence for Latin text
                                        )
                                    )
                                }
                            }
                            
                            lines.add(
                                TextLine(
                                    text = lineText,
                                    boundingBox = lineBoundingBox,
                                    elements = elements
                                )
                            )
                        }
                    }
                    
                    textBlocks.add(
                        TextBlock(
                            text = blockText,
                            boundingBox = blockBoundingBox,
                            lines = lines
                        )
                    )
                }
            }
            
            return if (textBlocks.isNotEmpty()) {
                OcrResult.Success(textBlocks)
            } else {
                OcrResult.Error("No text blocks found")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text blocks", e)
            return OcrResult.Error("Failed to extract text blocks: ${e.message}")
        }
    }

    /**
     * Helper function to await ML Kit result in a coroutine
     */
    private suspend fun <T> awaitTaskResult(task: Task<T>): T {
        return suspendCancellableCoroutine { continuation ->
            task
                .addOnSuccessListener { result ->
                    continuation.resume(result)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }

    private suspend fun awaitTextResult(task: Task<Text>): Text {
        return awaitTaskResult(task)
    }

    /**
     * Close the OCR processor and release resources
     */
    fun close() {
        try {
            textRecognizer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing text recognizer", e)
        }
    }
}

/**
 * Data classes for OCR results
 */
data class TextBlock(
    val text: String,
    val boundingBox: Rect,
    val lines: List<TextLine>
)

data class TextLine(
    val text: String,
    val boundingBox: Rect,
    val elements: List<TextElement>
)

data class TextElement(
    val text: String,
    val boundingBox: Rect,
    val confidence: Float
)

sealed class OcrResult {
    data class Success(val textBlocks: List<TextBlock>) : OcrResult()
    data class Error(val message: String) : OcrResult()
}