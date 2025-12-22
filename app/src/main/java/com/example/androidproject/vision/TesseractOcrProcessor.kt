package com.example.androidproject.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Alternative OCR implementation using Tesseract via tess-two
 * Provides fallback for ML Kit or additional processing options
 */
class TesseractOcrProcessor(private val context: Context) {
    companion object {
        private const val TAG = "TesseractOcrProcessor"
        private const val DATA_PATH = "/tessdata"
        private const val TESSERACT_LANGUAGE = "eng"
        private const val TESSERACT_DATA_PATH = "tesseract"
    }

    private var tessApi: TessBaseAPI? = null
    private var isInitialized = false

    /**
     * Initialize Tesseract OCR engine
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Check if tessdata exists, if not copy from assets
            val tessDataDir = File(context.filesDir, TESSERACT_DATA_PATH)
            if (!tessDataDir.exists()) {
                tessDataDir.mkdirs()
                copyTessDataFromAssets(tessDataDir)
            }

            // Initialize Tesseract API
            tessApi = TessBaseAPI().apply {
                init(tessDataDir.absolutePath + DATA_PATH, TESSERACT_LANGUAGE)
                setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
                setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!?@#$%&*()<>_-+=/:;'\".,[]{}|\\^~")
            }
            
            isInitialized = true
            Log.d(TAG, "Tesseract OCR initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Tesseract OCR", e)
            isInitialized = false
            false
        }
    }

    /**
     * Process bitmap and extract text with Tesseract
     */
    suspend fun processImage(bitmap: Bitmap): TesseractOcrResult = withContext(Dispatchers.IO) {
        if (!isInitialized || tessApi == null) {
            return@withContext TesseractOcrResult.Error("Tesseract not initialized")
        }

        try {
            // Set the bitmap for processing
            tessApi!!.setImage(bitmap)
            
            // Get the recognized text
            val recognizedText = tessApi!!.utff8Text
            
            // Get confidence values
            val confidence = tessApi!!.meanConfidence()
            
            // Extract bounding boxes for detected words
            val textBlocks = extractTextBlocks()
            
            Log.d(TAG, "Tesseract OCR completed. Confidence: $confidence")
            
            if (recognizedText.isNotBlank()) {
                TesseractOcrResult.Success(
                    text = recognizedText.trim(),
                    confidence = confidence,
                    textBlocks = textBlocks
                )
            } else {
                TesseractOcrResult.Error("No text detected")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image with Tesseract", e)
            TesseractOcrResult.Error("Processing failed: ${e.message}")
        }
    }

    /**
     * Process bitmap with custom ROI (Region of Interest)
     */
    suspend fun processImageWithRegion(bitmap: Bitmap, regions: List<android.graphics.Rect>): TesseractOcrResult {
        if (!isInitialized || tessApi == null) {
            return TesseractOcrResult.Error("Tesseract not initialized")
        }

        return withContext(Dispatchers.IO) {
            try {
                val allText = StringBuilder()
                val allBlocks = mutableListOf<TextBlock>()
                var totalConfidence = 0.0f
                var regionCount = 0

                for (region in regions) {
                    // Create cropped bitmap for region
                    val croppedBitmap = Bitmap.createBitmap(
                        bitmap,
                        region.left,
                        region.top,
                        region.width(),
                        region.height()
                    )

                    tessApi!!.setImage(croppedBitmap)
                    val regionText = tessApi!!.utff8Text.trim()
                    val regionConfidence = tessApi!!.meanConfidence()

                    if (regionText.isNotBlank()) {
                        allText.append(regionText).append(" ")
                        
                        // Extract text blocks for this region
                        val regionBlocks = extractTextBlocks()
                        regionBlocks.forEach { block ->
                            // Adjust bounding boxes to original image coordinates
                            val adjustedBlock = TextBlock(
                                text = block.text,
                                boundingBox = android.graphics.Rect(
                                    block.boundingBox.left + region.left,
                                    block.boundingBox.top + region.top,
                                    block.boundingBox.right + region.left,
                                    block.boundingBox.bottom + region.top
                                ),
                                lines = block.lines // Keep original lines
                            )
                            allBlocks.add(adjustedBlock)
                        }
                        
                        totalConfidence += regionConfidence
                        regionCount++
                    }
                }

                val averageConfidence = if (regionCount > 0) totalConfidence / regionCount else 0.0f

                Log.d(TAG, "Tesseract regional OCR completed. Average confidence: $averageConfidence")

                if (allText.isNotBlank()) {
                    TesseractOcrResult.Success(
                        text = allText.toString().trim(),
                        confidence = averageConfidence,
                        textBlocks = allBlocks
                    )
                } else {
                    TesseractOcrResult.Error("No text detected in specified regions")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing image regions with Tesseract", e)
                TesseractOcrResult.Error("Regional processing failed: ${e.message}")
            }
        }
    }

    /**
     * Extract text blocks with bounding boxes from Tesseract result
     */
    private fun extractTextBlocks(): List<TextBlock> {
        val blocks = mutableListOf<TextBlock>()
        
        try {
            val result = tessApi!!.result
            val iterator = result.words
            var currentBlock: TextBlock? = null
            
            while (iterator.next()) {
                val word = iterator.getUTF8Text()
                val confidence = iterator.confidence()
                val boundingBox = iterator.boundingBox()
                
                if (boundingBox != null) {
                    val rect = android.graphics.Rect(
                        boundingBox.x0,
                        boundingBox.y0,
                        boundingBox.x1,
                        boundingBox.y1
                    )
                    
                    // Group words into lines (simplified grouping)
                    if (currentBlock == null || !isInSameLine(currentBlock.boundingBox, rect)) {
                        currentBlock?.let { blocks.add(it) }
                        currentBlock = TextBlock(
                            text = word,
                            boundingBox = rect,
                            lines = listOf(
                                TextLine(
                                    text = word,
                                    boundingBox = rect,
                                    elements = listOf(
                                        TextElement(
                                            text = word,
                                            boundingBox = rect,
                                            confidence = confidence / 100.0f
                                        )
                                    )
                                )
                            )
                        )
                    } else {
                        // Append to current block
                        val newText = currentBlock.text + " " + word
                        val newRect = mergeRectangles(currentBlock.boundingBox, rect)
                        currentBlock = currentBlock.copy(
                            text = newText,
                            boundingBox = newRect,
                            lines = listOf(
                                TextLine(
                                    text = newText,
                                    boundingBox = newRect,
                                    elements = currentBlock.lines[0].elements + TextElement(
                                        text = word,
                                        boundingBox = rect,
                                        confidence = confidence / 100.0f
                                    )
                                )
                            )
                        )
                    }
                }
            }
            
            currentBlock?.let { blocks.add(it) }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text blocks", e)
        }
        
        return blocks
    }

    /**
     * Check if two rectangles are in the same text line (simplified)
     */
    private fun isInSameLine(rect1: android.graphics.Rect, rect2: android.graphics.Rect): Boolean {
        val verticalOverlap = Math.min(rect1.bottom, rect2.bottom) - Math.max(rect1.top, rect2.top)
        val minHeight = Math.min(rect1.height(), rect2.height())
        return verticalOverlap > minHeight * 0.5 // 50% overlap threshold
    }

    /**
     * Merge two rectangles into a larger one
     */
    private fun mergeRectangles(rect1: android.graphics.Rect, rect2: android.graphics.Rect): android.graphics.Rect {
        return android.graphics.Rect(
            Math.min(rect1.left, rect2.left),
            Math.min(rect1.top, rect2.top),
            Math.max(rect1.right, rect2.right),
            Math.max(rect1.bottom, rect2.bottom)
        )
    }

    /**
     * Copy Tesseract data files from assets
     */
    private suspend fun copyTessDataFromAssets(destDir: File) = withContext(Dispatchers.IO) {
        try {
            val tessDataPath = File(destDir, TESSERACT_DATA_PATH)
            if (!tessDataPath.exists()) {
                tessDataPath.mkdirs()
            }

            val files = context.assets.list("tesseract") ?: arrayOf()
            for (fileName in files) {
                copyAssetFile("tesseract/$fileName", File(tessDataPath, fileName))
            }
            
            Log.d(TAG, "Tesseract data files copied successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error copying Tesseract data files", e)
            throw e
        }
    }

    /**
     * Copy individual asset file
     */
    private suspend fun copyAssetFile(assetPath: String, destFile: File) = withContext(Dispatchers.IO) {
        context.assets.open(assetPath).use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                copyStream(inputStream, outputStream)
            }
        }
    }

    /**
     * Copy stream data
     */
    private suspend fun copyStream(input: InputStream, output: FileOutputStream) = withContext(Dispatchers.IO) {
        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
        }
        output.flush()
    }

    /**
     * Set Tesseract parameters for better recognition
     */
    fun setParameters(
        pageSegMode: Int = TessBaseAPI.PageSegMode.PSM_AUTO,
        charWhitelist: String? = null,
        charBlacklist: String? = null
    ) {
        tessApi?.setPageSegMode(pageSegMode)
        
        charWhitelist?.let {
            tessApi?.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, it)
        }
        
        charBlacklist?.let {
            tessApi?.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, it)
        }
    }

    /**
     * Close and release Tesseract resources
     */
    fun close() {
        try {
            tessApi?.end()
            isInitialized = false
            Log.d(TAG, "Tesseract OCR closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing Tesseract OCR", e)
        }
    }
}

/**
 * Tesseract OCR result sealed class
 */
sealed class TesseractOcrResult {
    data class Success(
        val text: String,
        val confidence: Float,
        val textBlocks: List<TextBlock>
    ) : TesseractOcrResult()
    
    data class Error(val message: String) : TesseractOcrResult()
}