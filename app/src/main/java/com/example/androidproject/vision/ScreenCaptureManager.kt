package com.example.androidproject.vision

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
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class ScreenCaptureManager(
    private val context: Context,
    private val onScreenStateChanged: (ScreenCaptureResult) -> Unit
) {
    companion object {
        private const val TAG = "ScreenCaptureManager"
        private const val SCREEN_CAPTURE_REQUEST_CODE = 1000
    }

    private val mediaProjectionManager: MediaProjectionManager = 
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    
    private var isCapturing = false
    private val _results = Channel<ScreenCaptureResult>(Channel.UNLIMITED)
    val results: Flow<ScreenCaptureResult> = _results.receiveAsFlow()

    /**
     * Creates a screen-capture intent via the system MediaProjectionManager for requesting user consent.
     *
     * This method prepares the Intent that must be launched from an Activity (e.g., via
     * startActivityForResult or an ActivityResultLauncher) to prompt the user to allow screen capture.
     * The intent is not started by this method; callers are responsible for launching it and handling
     * the activity result.
     */
    fun requestScreenCaptureConsent() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        // This should be launched from an Activity using startActivityForResult
        Log.d(TAG, "Requesting screen capture consent")
    }

    /**
     * Begins capturing the device screen using the provided Activity result data.
     *
     * Initializes a MediaProjection from the given result and sets up capture resources and a background
     * capture loop. On failure, posts an `ScreenCaptureResult.Error` to the manager's state callback.
     *
     * @param resultCode The result code returned by the screen capture consent Activity.
     * @param data The Intent data returned by the screen capture consent Activity.
     */
    fun startCapture(resultCode: Int, data: Intent) {
        if (isCapturing) {
            Log.w(TAG, "Capture already in progress")
            return
        }

        try {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            setupCapture()
            startCaptureLoop()
            isCapturing = true
            Log.i(TAG, "Screen capture started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start capture", e)
            onScreenStateChanged(ScreenCaptureResult.Error("Failed to start capture: ${e.message}"))
        }
    }

    /**
     * Stops active screen capture and releases all associated resources.
     *
     * Sets the capturing flag to false, cancels pending capture callbacks, stops the capture thread,
     * releases the virtual display and media projection, closes the image reader, and clears internal
     * references. Any exceptions thrown during teardown are caught and ignored to ensure cleanup
     * completes.
     */
    fun stopCapture() {
        isCapturing = false
        
        try {
            captureHandler?.removeCallbacksAndMessages(null)
            captureThread?.quitSafely()
            
            virtualDisplay?.release()
            mediaProjection?.stop()
            imageReader?.close()
            
            Log.i(TAG, "Screen capture stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capture", e)
        } finally {
            captureThread = null
            captureHandler = null
            virtualDisplay = null
            mediaProjection = null
            imageReader = null
        }
    }

    /**
 * Indicates whether screen capture is currently active.
 *
 * @return `true` if capture is active, `false` otherwise.
 */
    fun isCapturing(): Boolean = isCapturing

    /**
     * Initializes capture resources for screen recording: creates an ImageReader sized to the
     * current display, registers its image-available listener to process incoming frames, and
     * creates a VirtualDisplay that streams the screen into the ImageReader's surface.
     *
     * This configures `imageReader` and `virtualDisplay` and runs the image listener on
     * `captureHandler`. The listener acquires the latest image and delegates processing to
     * `processCapturedImage`.
     */
    private fun setupCapture() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        // Create ImageReader for capturing screenshots
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        imageReader?.setOnImageAvailableListener(
            { reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    processCapturedImage(image)
                    image.close()
                }
            },
            captureHandler
        )

        // Create VirtualDisplay
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            captureHandler
        )
    }

    /**
     * Starts a background HandlerThread named "ScreenCaptureThread" and creates a Handler
     * associated with its looper for processing screen capture events.
     */
    private fun startCaptureLoop() {
        captureThread = HandlerThread("ScreenCaptureThread").apply {
            start()
            captureHandler = Handler(looper)
        }
    }

    /**
     * Converts a captured Image into a Bitmap, crops any row padding, and publishes the result.
     *
     * On success sends a ScreenCaptureResult.Success containing the cropped bitmap to the internal results
     * channel and invokes the onScreenStateChanged callback. On failure sends a ScreenCaptureResult.Error
     * with a message and logs the exception.
     *
     * @param image The captured Android Image provided by the ImageReader listener.
     */
    private fun processCapturedImage(image: Image) {
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            // Create bitmap from buffer
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            // Crop to remove padding
            val croppedBitmap = if (rowPadding != 0) {
                Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    image.width,
                    image.height
                )
            } else {
                bitmap
            }

            val result = ScreenCaptureResult.Success(croppedBitmap)
            _results.trySend(result)
            onScreenStateChanged(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing captured image", e)
            _results.trySend(ScreenCaptureResult.Error("Failed to process image: ${e.message}"))
        }
    }

    /**
     * Encode a bitmap as a JPEG byte array using 80% quality.
     *
     * @param bitmap The bitmap to encode.
     * @return A byte array containing the JPEG-compressed image data at 80% quality.
     */
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return outputStream.toByteArray()
    }
}

/**
 * Screen capture result sealed class
 */
sealed class ScreenCaptureResult {
    data class Success(val bitmap: Bitmap) : ScreenCaptureResult()
    data class Error(val message: String) : ScreenCaptureResult()
}