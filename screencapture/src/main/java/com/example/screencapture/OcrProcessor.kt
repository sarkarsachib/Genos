package com.example.screencapture

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

open class OcrProcessor(
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
) {
    /**
     * Extracts text blocks from the given bitmap using the configured TextRecognizer and delivers them via the callback.
     *
     * Processes the bitmap with ML Kit's text recognizer and invokes `callback` with `Result.success` containing a list
     * of `TextBlock` objects (each with recognized text and its bounding box) on success, or `Result.failure`
     * with the recognition exception on error.
     *
     * @param bitmap The image to run OCR on; expected to be an upright Bitmap (rotation 0).
     * @param callback Receives the recognition outcome as a `Result` — on success a `List<TextBlock>`, on failure the thrown exception.
     */
    open fun process(bitmap: Bitmap, callback: (Result<List<TextBlock>>) -> Unit) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val textBlocks = visionText.textBlocks.map { block ->
                    TextBlock(
                        text = block.text,
                        boundingBox = block.boundingBox
                    )
                }
                callback(Result.success(textBlocks))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }
}