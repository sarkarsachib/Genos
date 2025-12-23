package com.example.screencapture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureManager(
    private val context: Context,
    private val ocrProcessor: OcrProcessor = OcrProcessor()
) {

    private val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var isCapturing = AtomicBoolean(false)
    private var streamCallback: ((Result<CaptureResult>) -> Unit)? = null
    
    // Config
    private var density: Int = 0
    private var width: Int = 0
    private var height: Int = 0

    companion object {
        private const val TAG = "ScreenCaptureManager"
        private const val VIRTUAL_DISPLAY_NAME = "ScreenCapture"
    }

    /**
     * Helper to launch the permission intent.
     * The Activity must handle onActivityResult and pass the result back to initialize().
     */
    fun requestScreenCapturePermission(activity: Activity, requestCode: Int) {
        activity.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), requestCode)
    }

    /**
     * Initialize the session with the permission result.
     */
    fun startSession(resultCode: Int, data: Intent, screenWidth: Int, screenHeight: Int, screenDensity: Int) {
        if (resultCode != Activity.RESULT_OK) {
            throw SecurityException("User denied screen capture permission")
        }
        
        stopSession() // Clean up any existing session
        
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                stopSession()
            }
        }, Handler(Looper.getMainLooper()))
        
        this.width = screenWidth
        this.height = screenHeight
        this.density = screenDensity
        
        setupVirtualDisplay()
    }

    private fun setupVirtualDisplay() {
        if (mediaProjection == null) return

        // Using RGBA_8888 for compatibility with ML Kit and generic Bitmap usage
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    /**
     * Capture a single frame, process it, and return the result.
     */
    fun captureOnce(callback: (Result<CaptureResult>) -> Unit) {
        if (mediaProjection == null) {
            callback(Result.failure(IllegalStateException("MediaProjection not initialized. Call startSession first.")))
            return
        }

        // We need to wait for a valid image
        scope.launch {
            try {
                // Short delay to ensure VirtualDisplay has pushed a frame if just started
                delay(100) 
                
                val bitmap = acquireLatestBitmap()
                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        callback(Result.failure(RuntimeException("Failed to acquire bitmap from ImageReader")))
                    }
                    return@launch
                }
                
                processBitmap(bitmap, callback)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(Result.failure(e))
                }
            }
        }
    }

    /**
     * Start a continuous stream of captures.
     * Note: This implementation pulls frames as fast as possible or could be throttled.
     * For accessibility events, one might trigger captureOnce externally.
     * Here we implement a simple loop or listener based approach.
     */
    fun startStream(periodMs: Long = 1000, callback: (Result<CaptureResult>) -> Unit) {
        if (isCapturing.get()) return
        isCapturing.set(true)
        streamCallback = callback
        
        scope.launch {
            while (isCapturing.get() && isActive) {
                val bitmap = acquireLatestBitmap()
                if (bitmap != null) {
                    processBitmap(bitmap) { result ->
                        // Pass result to stream callback
                        streamCallback?.invoke(result)
                    }
                }
                delay(periodMs)
            }
        }
    }

    fun stopStream() {
        isCapturing.set(false)
        streamCallback = null
    }

    fun stopSession() {
        stopStream()
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun acquireLatestBitmap(): Bitmap? {
        val image = imageReader?.acquireLatestImage() ?: return null
        
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width
        
        // Create bitmap
        val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()
        
        // Handle potential padding/stride mismatch by cropping if necessary
        // Often expected width matches, but sometimes stride adds padding.
        if (bitmap.width != width) {
             return Bitmap.createBitmap(bitmap, 0, 0, width, height)
        }
        
        return bitmap
    }

    private fun processBitmap(bitmap: Bitmap, callback: (Result<CaptureResult>) -> Unit) {
        ocrProcessor.process(bitmap) { result ->
             result.onSuccess { textBlocks ->
                 val captureResult = CaptureResult(bitmap, textBlocks)
                 // Callback on Main Thread
                 Handler(Looper.getMainLooper()).post {
                     callback(Result.success(captureResult))
                 }
             }.onFailure { e ->
                 Handler(Looper.getMainLooper()).post {
                     callback(Result.failure(e))
                 }
             }
        }
    }
}
