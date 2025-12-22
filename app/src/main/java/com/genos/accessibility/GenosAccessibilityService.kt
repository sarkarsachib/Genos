package com.genos.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.genos.accessibility.aggregator.ScreenStateAggregator
import com.genos.accessibility.model.UiTree
import com.genos.accessibility.model.toUiElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GenosAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "GenosAccessibilityService"
        const val ENABLE_LOGGING = true
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val screenStateAggregator = ScreenStateAggregator()
    
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    
    private val _latestUiTree = MutableStateFlow<UiTree?>(null)
    val latestUiTree: StateFlow<UiTree?> = _latestUiTree.asStateFlow()
    
    private var currentPackageName: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "GenosAccessibilityService created")
        
        _isServiceRunning.value = true
        
        serviceScope.launch {
            screenStateAggregator.currentUiTree.collect { uiTree ->
                _latestUiTree.value = uiTree
            }
        }
        
        serviceScope.launch {
            screenStateAggregator.appTransitionEvents.collect { event ->
                event?.let {
                    Log.i(TAG, "App transition: ${it.previousPackage} -> ${it.newPackage}")
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "GenosAccessibilityService connected")
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
            packageNames = null
        }
        
        serviceInfo = info
        _isServiceRunning.value = true
        Log.i(TAG, "Accessibility service configured and ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            val eventType = event.eventType
            val packageName = event.packageName?.toString()
            
            if (ENABLE_LOGGING) {
                Log.d(TAG, "Accessibility event: type=$eventType, package=$packageName")
            }
            
            if (packageName != null && packageName != currentPackageName) {
                currentPackageName = packageName
                screenStateAggregator.onAccessibilityEvent(packageName)
                Log.i(TAG, "Package changed: $packageName")
            }
            
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                
                val rootNode = try {
                    rootInActiveWindow
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting root node: ${e.message}")
                    null
                }
                
                if (rootNode != null) {
                    processUiTree(rootNode, packageName)
                } else if (ENABLE_LOGGING) {
                    Log.w(TAG, "Root node is null for package: $packageName")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event: ${e.message}", e)
        }
    }

    private fun processUiTree(rootNode: AccessibilityNodeInfo, packageName: String?) {
        try {
            val uiTree = rootNode.toUiElement()
            val fullTree = UiTree(
                root = uiTree,
                packageName = packageName ?: "unknown",
                timestamp = System.currentTimeMillis()
            )
            
            screenStateAggregator.onUiTreeUpdate(fullTree)
            
            if (ENABLE_LOGGING) {
                logUiTreeSnapshot(uiTree, packageName)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing UI tree: ${e.message}", e)
        }
    }

    private fun logUiTreeSnapshot(element: UiElement, packageName: String?, depth: Int = 0) {
        val indent = "  ".repeat(depth)
        val elementInfo = buildString {
            append("$indent[${element.className ?: "Unknown"}]")
            element.text?.let { append(" text=\"$it\"") }
            element.contentDescription?.let { append(" desc=\"$it\"") }
            element.viewIdResourceName?.let { append(" id=$it") }
            append(" clickable=${element.isClickable}")
            append(" bounds=${element.bounds}")
        }
        
        Log.d(TAG, elementInfo)
        
        element.children.forEach { child ->
            logUiTreeSnapshot(child, packageName, depth + 1)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "GenosAccessibilityService interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "GenosAccessibilityService onUnbind")
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.i(TAG, "GenosAccessibilityService destroyed")
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        _isServiceRunning.value = false
        screenStateAggregator.clear()
        serviceScope.cancel()
        Log.i(TAG, "GenosAccessibilityService cleanup completed")
    }

    fun getCurrentUiTree(): UiTree? = screenStateAggregator.getCurrentUiTreeSnapshot()
    
    fun getCurrentAppPackage(): String? = screenStateAggregator.currentAppPackage.value
}