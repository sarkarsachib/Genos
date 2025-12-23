package ai.genos.core.input

import android.util.Log

class InputProcessor {

    companion object {
        private const val TAG = "InputProcessor"
    }

    fun processInput(text: String): String {
        Log.d(TAG, "Process input: $text - placeholder implementation")
        return text
    }

    fun handleVoiceInput(audioData: ByteArray) {
        Log.d(TAG, "Handle voice input - placeholder implementation")
    }
}
