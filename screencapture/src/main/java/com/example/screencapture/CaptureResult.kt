package com.example.screencapture

import android.graphics.Bitmap
import android.graphics.Rect

data class CaptureResult(
    val bitmap: Bitmap,
    val textBlocks: List<TextBlock>,
    val timestamp: Long = System.currentTimeMillis()
)

data class TextBlock(
    val text: String,
    val boundingBox: Rect?
)
