package com.example.androidproject.vision

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import com.example.androidproject.accessibility.MyAccessibilityService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ScreenCaptureCoordinator(
    private val context: Context,
    private val activity: Activity
) {
    companion object {
        private const val TAG = "ScreenCaptureCoordinator"
        private const val SCREEN_CAPTURE_REQUEST_CODE = 1000
    }

    private val screenCaptureManager = ScreenCaptureManager(context, ::onScreenCaptureResult)
    private val ocrProcessor = OcrProcessor()
    private val screenStateAggregator = ScreenStateAggregator(context)
    
    private var accessibilityService: MyAccessibilityService? = null
    private var isActive = false
    
    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Initializes the screen capture pipeline by ensuring required accessibility permission and requesting screen capture consent.
     *
     * If accessibility permission is not granted, the function will prompt the system accessibility settings and defer continuation until the permission flow completes.
     *
     * @return `true` if screen capture consent was requested successfully, `false` otherwise.
     */
    suspend fun initializeScreenCapture(): Boolean = withContext(Dispatchers.Main) {
        return@withContext suspendCoroutine { continuation ->
            try {
                // Request accessibility service permission
                if (!isAccessibilityServiceEnabled()) {
                    requestAccessibilityServicePermission()
                    // The flow will continue in onAccessibilityPermissionResult
                    return@suspendCoroutine
                }
                
                // Request screen capture consent
                requestScreenCaptureConsent()
                continuation.resume(true)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing screen capture", e)
                continuation.resume(false)
            }
        }
    }

    /**
     * Starts the screen capture pipeline, enabling accessibility tree collection and screen-change monitoring.
     *
     * Sets the coordinator to active, initiates accessibility tree collection when available, begins screen capture monitoring, and starts periodic accessibility tree collection. On failure the coordinator resets to inactive.
     */
    suspend fun startPipeline() {
        if (isActive) {
            Log.w(TAG, "Pipeline already active")
            return
        }
        
        try {
            isActive = true
            
            // Start accessibility tree collection if service is available
            accessibilityService?.startTreeCollection()
            
            // Start capturing screen changes
            startScreenCapture()
            
            // Collect accessibility tree periodically
            startAccessibilityCollection()
            
            Log.i(TAG, "Screen capture pipeline started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting pipeline", e)
            isActive = false
        }
    }

    /**
     * Stops the running screen capture pipeline and releases its resources.
     *
     * If the pipeline is not active this function does nothing. When active, it marks the
     * pipeline inactive, stops the screen capture manager, stops accessibility tree collection
     * if a service is attached, and cancels the coordinator's coroutine scope.
     */
    fun stopPipeline() {
        if (!isActive) return
        
        try {
            isActive = false
            
            screenCaptureManager.stopCapture()
            accessibilityService?.stopTreeCollection()
            coordinatorScope.cancel()
            
            Log.i(TAG, "Screen capture pipeline stopped")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping pipeline", e)
        }
    }

    /**
     * Captures a single screen frame, runs OCR on it, and aggregates the resulting screen state.
     *
     * Returns the aggregated ScreenStateResult when a bitmap is captured and processed; returns `null`
     * if the pipeline is not active, no frame could be captured, or an error occurred during capture or processing.
     *
     * @return The aggregated screen state, or `null` if capture/processing failed or the pipeline is inactive.
     */
    suspend fun triggerManualCapture(): ScreenStateResult? {
        if (!isActive) {
            Log.w(TAG, "Pipeline not active, cannot trigger manual capture")
            return null
        }
        
        return try {
            // Start a one-time capture
            val bitmap = captureSingleFrame()
            bitmap?.let {
                val ocrResult = ocrProcessor.processImage(it)
                screenStateAggregator.aggregateScreenStateSimple(it, ocrResult)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering manual capture", e)
            null
        }
    }

    /**
     * Checks whether this app's accessibility service is listed in the system's enabled accessibility services.
     *
     * @return `true` if the current package name appears in `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`, `false` otherwise.
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        return enabledServices?.contains(context.packageName) == true
    }

    /**
     * Opens the system Accessibility Settings screen so the user can enable the app's accessibility service.
     */
    private fun requestAccessibilityServicePermission() {
        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Starts the system flow to request user consent for screen capture.
     *
     * The Activity is expected to handle the resulting intent result (for example via startActivityForResult
     * or the Activity Result API).
     */
    private fun requestScreenCaptureConsent() {
        screenCaptureManager.requestScreenCaptureConsent()
        // Note: This should be handled by the Activity with startActivityForResult
    }

    /**
     * Handle the result of the screen capture consent flow and start capture when consent is provided.
     *
     * Starts the screen capture using the provided activity result data; if `data` is null, no capture is started.
     *
     * @param resultCode The result code returned by the consent activity (for example, `Activity.RESULT_OK`).
     * @param data The `Intent` containing the permission token required to start screen capture, or `null` if not provided.
     */
    fun onScreenCaptureConsentResult(resultCode: Int, data: Intent?) {
        data?.let {
            coordinatorScope.launch {
                screenCaptureManager.startCapture(resultCode, it)
            }
        }
    }

    /**
     * Attach or clear the accessibility service used by the coordinator.
     *
     * @param service The MyAccessibilityService instance to use, or `null` to remove the current reference.
     */
    fun setAccessibilityService(service: MyAccessibilityService?) {
        accessibilityService = service
    }

    /**
 * Reports whether the screen capture pipeline is currently active.
 *
 * @return `true` if the pipeline is active, `false` otherwise.
 */
    fun isPipelineActive(): Boolean = isActive

    /**
     * Handles a screen capture result by processing successful captures or logging errors.
     *
     * If `result` is `Success`, schedules processing of the captured bitmap on the coordinator scope.
     * If `result` is `Error`, logs the error message.
     *
     * @param result The outcome of a screen capture operation.
     */
    private fun onScreenCaptureResult(result: ScreenCaptureResult) {
        when (result) {
            is ScreenCaptureResult.Success -> {
                coordinatorScope.launch {
                    processCapturedFrame(result.bitmap)
                }
            }
            is ScreenCaptureResult.Error -> {
                Log.e(TAG, "Screen capture error: ${result.message}")
            }
        }
    }

    /**
     * Processes a captured screen bitmap by extracting text, collecting the current accessibility tree,
     * aggregating those inputs into a screen state, and dispatching the aggregated result.
     *
     * The function performs OCR on the provided bitmap, attempts to obtain an accessibility tree
     * (empty if unavailable), passes the bitmap, OCR result, and accessibility tree to the
     * ScreenStateAggregator, and then forwards the aggregation outcome to the coordinator's handler.
     *
     * @param bitmap The captured screen image to process.
     */
    private suspend fun processCapturedFrame(bitmap: Bitmap) {
        try {
            // Process OCR on the captured frame
            val ocrResult = ocrProcessor.processImage(bitmap)
            
            // Get current accessibility tree
            val accessibilityTree = accessibilityService?.let { service ->
                withContext(Dispatchers.IO) {
                    // This would need to be implemented to get the current tree
                    emptyList<com.example.androidproject.accessibility.AccessibilityTreeNode>()
                }
            } ?: emptyList()
            
            // Aggregate the screen state
            val screenStateResult = screenStateAggregator.aggregateScreenState(
                bitmap,
                ocrResult,
                accessibilityTree
            )
            
            // Notify about the aggregated result
            onScreenStateAggregated(screenStateResult)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing captured frame", e)
        }
    }

    /**
     * Begins monitoring the device screen for changes to enable periodic capture of frames.
     */
    private suspend fun startScreenCapture() {
        // Screen capture will automatically trigger on screen changes
        // The ScreenCaptureManager handles the periodic captures
        Log.d(TAG, "Started screen capture monitoring")
    }

    /**
     * Begins collecting accessibility tree updates and handling each received tree.
     *
     * Starts a coroutine on the coordinator scope that subscribes to the accessibility service's
     * `accessibilityTreeFlow` and logs each emitted tree's node count. If no accessibility service
     * is set, the function does nothing.
     */
    private fun startAccessibilityCollection() {
        coordinatorScope.launch {
            accessibilityService?.accessibilityTreeFlow?.collectLatest { tree ->
                Log.d(TAG, "Received accessibility tree with ${tree.size} nodes")
                // Process accessibility tree updates if needed
            }
        }
    }

    /**
     * Captures a single screen frame as a Bitmap.
     *
     * @return A Bitmap containing the captured frame, or `null` if a capture cannot be performed
     * (for example when MediaProjection is not configured or permission is missing).
     */
    private suspend fun captureSingleFrame(): Bitmap? {
        // This would be implemented to capture a single frame on demand
        // For now, returning null as it requires MediaProjection setup
        return null
    }

    /**
     * Handles the completed screen state aggregation result.
     *
     * Processes a successful aggregation (e.g., transmit or further process the aggregated data)
     * and records errors when aggregation fails.
     *
     * @param result The aggregated screen state result to handle; may be `ScreenStateResult.Success` or `ScreenStateResult.Error`.
     */
    private fun onScreenStateAggregated(result: ScreenStateResult) {
        when (result) {
            is ScreenStateResult.Success -> {
                Log.d(TAG, "Screen state aggregated successfully")
                // Here you would transmit to Gemini or process the aggregated data
            }
            is ScreenStateResult.Error -> {
                Log.e(TAG, "Error aggregating screen state: ${result.message}")
            }
        }
    }

    /**
     * Releases resources used by the coordinator and stops any running pipeline.
     *
     * Stops the active pipeline if running, closes the OCR processor, and cancels the coordinator's coroutine scope.
     */
    fun cleanup() {
        stopPipeline()
        ocrProcessor.close()
        coordinatorScope.cancel()
    }
}