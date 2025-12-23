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
     * Prepare and initialize the Tesseract OCR engine, ensuring tessdata files are present.
     *
     * This will create the app's tessdata directory and copy bundled tessdata from assets if missing,
     * instantiate and configure the TessBaseAPI, and update the processor's initialization state.
     *
     * @return `true` if initialization succeeded and the engine is ready, `false` otherwise.
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
     * Perform OCR on the provided bitmap using the initialized Tesseract engine and return recognized text and layout.
     *
     * @param bitmap The image to analyze for text.
     * @return `TesseractOcrResult.Success` containing the trimmed recognized text, the mean confidence value, and the list of detected text blocks when text is found; `TesseractOcrResult.Error` with a descriptive message otherwise (for example, when Tesseract is not initialized or processing fails).
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
     * Performs OCR on specific rectangular regions of the provided bitmap and aggregates the results.
     *
     * Processes each region in bitmap coordinates, runs OCR on the cropped area, and accumulates recognized
     * text, adjusted text blocks (bounding boxes translated to the original bitmap coordinate space),
     * and an average confidence across regions that produced text.
     *
     * @param bitmap The source image containing the regions to process.
     * @param regions List of rectangular regions in the bitmap's coordinate space to run OCR on.
     * @return `TesseractOcrResult.Success` containing the concatenated text, the average confidence across
     *         regions that yielded text, and the list of text blocks with bounding boxes adjusted to the
     *         original bitmap coordinates; `TesseractOcrResult.Error` with a message if processing failed
     *         or no text was detected in the specified regions.
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
     * Builds a list of text blocks with bounding boxes and confidence scores from the current Tesseract result.
     *
     * Each returned TextBlock represents one or more words grouped into an approximate single-line block; each block contains
     * one TextLine (the assembled line) which contains TextElement entries for individual words. Confidence values on
     * TextElement are normalized to the 0.0–1.0 range.
     *
     * @return A list of extracted TextBlock objects; returns an empty list if no blocks are found or if extraction fails.
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
     * Determines whether two rectangles belong to the same text line based on vertical overlap.
     *
     * @param rect1 First rectangle in image coordinates.
     * @param rect2 Second rectangle in image coordinates.
     * @return `true` if the vertical overlap between the rectangles is greater than 50% of the smaller rectangle's height, `false` otherwise.
     */
    private fun isInSameLine(rect1: android.graphics.Rect, rect2: android.graphics.Rect): Boolean {
        val verticalOverlap = Math.min(rect1.bottom, rect2.bottom) - Math.max(rect1.top, rect2.top)
        val minHeight = Math.min(rect1.height(), rect2.height())
        return verticalOverlap > minHeight * 0.5 // 50% overlap threshold
    }

    /**
     * Creates the minimal bounding rectangle that contains both input rectangles.
     *
     * @param rect1 First rectangle to merge.
     * @param rect2 Second rectangle to merge.
     * @return A new `Rect` that minimally bounds `rect1` and `rect2`.
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
     * Copies required Tesseract `tessdata` files from the app assets into the provided destination directory.
     *
     * Creates a `tessdata` subdirectory under [destDir] if it does not exist and copies all files found in the
     * `assets/tesseract` folder into that location.
     *
     * @param destDir The base destination directory under which a `tessdata` folder will be created and populated.
     * @throws Exception If any IO or asset access error occurs during copying; the exception is propagated to the caller.
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
     * Copies a file from the app's assets to the given destination file.
     *
     * @param assetPath Path to the asset inside the app's assets directory.
     * @param destFile Destination file on the filesystem to write the asset contents to.
     */
    private suspend fun copyAssetFile(assetPath: String, destFile: File) = withContext(Dispatchers.IO) {
        context.assets.open(assetPath).use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                copyStream(inputStream, outputStream)
            }
        }
    }

    /**
     * Copies all bytes from the given input stream to the provided output stream and flushes the output.
     *
     * @param input Source input stream to read bytes from.
     * @param output Destination output stream to write bytes to; will be flushed after copying.
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
     * Configure Tesseract's page segmentation mode and optional character whitelist/blacklist.
     *
     * @param pageSegMode Page segmentation mode constant (use values from `TessBaseAPI.PageSegMode`) that controls how Tesseract analyzes the image layout.
     * @param charWhitelist Characters that Tesseract should allow during recognition; pass `null` to leave the whitelist unchanged.
     * @param charBlacklist Characters that Tesseract should disallow during recognition; pass `null` to leave the blacklist unchanged.
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
     * Releases Tesseract resources and marks the processor as uninitialized.
     *
     * Safe to call multiple times; subsequent calls have no effect if already closed.
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