package ai.genos.core.capture

import android.graphics.Bitmap
import android.util.Log

class TextRecognizer {

    companion object {
        private const val TAG = "TextRecognizer"
    }

    fun recognizeText(bitmap: Bitmap): String {
        Log.d(TAG, "Recognize text from bitmap - placeholder implementation")
        return ""
    }

    fun recognizeTextWithMLKit(bitmap: Bitmap, callback: (String) -> Unit) {
        Log.d(TAG, "Recognize text with ML Kit - placeholder implementation")
        callback("")
    }

    fun recognizeTextWithTesseract(bitmap: Bitmap): String {
        Log.d(TAG, "Recognize text with Tesseract - placeholder implementation")
        return ""
    }
}
