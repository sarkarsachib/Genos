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
     * Starts the system screen-capture permission flow by launching the MediaProjection intent.
     *
     * The calling Activity must handle onActivityResult and pass the resulting data to startSession
     * to complete initialization.
     *
     * @param activity The Activity used to start the permission intent.
     * @param requestCode The request code to identify the permission result in onActivityResult.
     */
    fun requestScreenCapturePermission(activity: Activity, requestCode: Int) {
        activity.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), requestCode)
    }

    /**
     * Start a screen capture session using the provided MediaProjection permission result.
     *
     * Stops any existing session, obtains a MediaProjection from the supplied result, registers a stop callback,
     * stores the requested capture dimensions and density, and initializes the virtual display and image reader.
     *
     * @param resultCode The result code returned to the Activity's onActivityResult.
     * @param data The Intent data returned to onActivityResult containing the permission grant.
     * @param screenWidth The target capture width in pixels.
     * @param screenHeight The target capture height in pixels.
     * @param screenDensity The display density (DPI) to use for the virtual display.
     * @throws SecurityException if the permission result is not RESULT_OK.
     */
    fun startSession(resultCode: Int, data: Intent, screenWidth: Int, screenHeight: Int, screenDensity: Int) {
        if (resultCode != Activity.RESULT_OK) {
            throw SecurityException("User denied screen capture permission")
        }
        
        stopSession() // Clean up any existing session
        
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            /**
             * Stops any active screen capture session and releases related resources when the host is stopped.
             *
             * Ensures MediaProjection, VirtualDisplay, ImageReader, and any active streams are terminated by
             * delegating to stopSession().
             */
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

    /**
     * Initializes the ImageReader and VirtualDisplay used for screen capture.
     *
     * If no MediaProjection is available, the method returns without changing state.
     *
     * The created ImageReader is configured for RGBA_8888 and the VirtualDisplay is
     * attached to the ImageReader's surface; both are stored on the instance for use
     * by capture methods.
     */
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
     * Capture a single screen frame, run OCR processing on it, and deliver the result via the callback.
     *
     * If the MediaProjection session is not started, the callback is invoked with a failure.
     * The callback is always invoked on the main thread with either a successful CaptureResult or a failure describing what went wrong (for example, failure to acquire a bitmap or processing errors).
     *
     * @param callback Receives a `Result<CaptureResult>`: `Result.success` contains the processed capture (bitmap and OCR blocks); `Result.failure` contains the encountered exception.
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
     * Starts a periodic screen-capture stream that processes each captured frame and reports results to the provided callback.
     *
     * If a stream is already running this call is a no-op. The callback is invoked on the main thread with OCR processing results for each captured frame.
     *
     * @param periodMs Interval in milliseconds between capture attempts. Defaults to 1000 (1 second).
     * @param callback Receives a `Result<CaptureResult>` for each processed frame; success contains the capture result, failure contains the error.
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

    /**
     * Stops the ongoing screen capture stream and clears the callback used to deliver results.
     *
     * After calling this, no further frames will be captured or forwarded to the previous callback.
     */
    fun stopStream() {
        isCapturing.set(false)
        streamCallback = null
    }

    /**
     * Stops capture activity and releases all media-projection resources.
     *
     * Stops any active capture stream, releases and clears the virtual display and image reader,
     * and stops the media projection instance.
     */
    fun stopSession() {
        stopStream()
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    /**
     * Retrieves the most recent image from the ImageReader and converts it to a Bitmap sized to the configured width and height.
     *
     * The underlying Image (if any) is consumed and closed; if the image plane contains row padding, the returned Bitmap is cropped to the target width and height.
     *
     * @return A Bitmap of dimensions `width x height` containing the latest captured frame, or `null` if no image was available.
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

    /**
     * Processes a captured bitmap with the OCR processor and delivers a CaptureResult (or failure) to the provided callback on the main thread.
     *
     * @param bitmap The captured bitmap to be analyzed by the OCR processor.
     * @param callback Receives a `Result` containing a `CaptureResult` on success or the exception on failure; always invoked on the main thread.
     */
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