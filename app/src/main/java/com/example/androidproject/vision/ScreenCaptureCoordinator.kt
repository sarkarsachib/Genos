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
     * Initialize screen capture pipeline with user consent
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
     * Start the complete pipeline
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
     * Stop the pipeline
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
     * Manual capture trigger
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
     * Check if accessibility service is enabled
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        return enabledServices?.contains(context.packageName) == true
    }

    /**
     * Request accessibility service permission
     */
    private fun requestAccessibilityServicePermission() {
        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Request screen capture consent
     */
    private fun requestScreenCaptureConsent() {
        screenCaptureManager.requestScreenCaptureConsent()
        // Note: This should be handled by the Activity with startActivityForResult
    }

    /**
     * Handle screen capture consent result from Activity
     */
    fun onScreenCaptureConsentResult(resultCode: Int, data: Intent?) {
        data?.let {
            coordinatorScope.launch {
                screenCaptureManager.startCapture(resultCode, it)
            }
        }
    }

    /**
     * Set accessibility service instance
     */
    fun setAccessibilityService(service: MyAccessibilityService?) {
        accessibilityService = service
    }

    /**
     * Check if pipeline is active
     */
    fun isPipelineActive(): Boolean = isActive

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

    private suspend fun startScreenCapture() {
        // Screen capture will automatically trigger on screen changes
        // The ScreenCaptureManager handles the periodic captures
        Log.d(TAG, "Started screen capture monitoring")
    }

    private fun startAccessibilityCollection() {
        coordinatorScope.launch {
            accessibilityService?.accessibilityTreeFlow?.collectLatest { tree ->
                Log.d(TAG, "Received accessibility tree with ${tree.size} nodes")
                // Process accessibility tree updates if needed
            }
        }
    }

    private suspend fun captureSingleFrame(): Bitmap? {
        // This would be implemented to capture a single frame on demand
        // For now, returning null as it requires MediaProjection setup
        return null
    }

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
     * Clean up resources
     */
    fun cleanup() {
        stopPipeline()
        ocrProcessor.close()
        coordinatorScope.cancel()
    }
}