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
