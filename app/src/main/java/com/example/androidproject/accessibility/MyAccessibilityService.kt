package com.example.androidproject.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class MyAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "MyAccessibilityService"
    }
    
    private val _accessibilityTree = Channel<List<AccessibilityTreeNode>>(Channel.UNLIMITED)
    val accessibilityTreeFlow: Flow<List<AccessibilityTreeNode>> = _accessibilityTree.receiveAsFlow()
    
    private var isCollectingTree = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || 
            event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            if (isCollectingTree) {
                collectAccessibilityTree()
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }
    
    /**
     * Start collecting accessibility tree data
     */
    fun startTreeCollection() {
        isCollectingTree = true
        collectAccessibilityTree()
        Log.d(TAG, "Started accessibility tree collection")
    }
    
    /**
     * Stop collecting accessibility tree data
     */
    fun stopTreeCollection() {
        isCollectingTree = false
        Log.d(TAG, "Stopped accessibility tree collection")
    }
    
    /**
     * Collect current accessibility tree
     */
    private fun collectAccessibilityTree() {
        try {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val tree = AccessibilityTreeNode.fromNodeInfo(rootNode)
                _accessibilityTree.trySend(tree)
                Log.d(TAG, "Collected accessibility tree with ${tree.size} nodes")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting accessibility tree", e)
        }
    }
}

/**
 * Data class representing an accessibility node in the tree
 */
data class AccessibilityTreeNode(
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val bounds: Rect,
    val isClickable: Boolean,
    val isFocusable: Boolean,
    val isEnabled: Boolean,
    val isVisible: Boolean,
    val resourceId: String?,
    val packageName: String?,
    val parent: AccessibilityTreeNode? = null,
    val children: List<AccessibilityTreeNode> = emptyList()
) {
    
    companion object {
        /**
         * Create accessibility tree from AccessibilityNodeInfo
         */
        fun fromNodeInfo(nodeInfo: AccessibilityNodeInfo, parent: AccessibilityTreeNode? = null): List<AccessibilityTreeNode> {
            val nodes = mutableListOf<AccessibilityTreeNode>()
            
            val bounds = Rect()
            nodeInfo.getBoundsInScreen(bounds)
            
            val treeNode = AccessibilityTreeNode(
                className = nodeInfo.className?.toString(),
                text = nodeInfo.text?.toString(),
                contentDescription = nodeInfo.contentDescription?.toString(),
                bounds = bounds,
                isClickable = nodeInfo.isClickable,
                isFocusable = nodeInfo.isFocusable,
                isEnabled = nodeInfo.isEnabled,
                isVisible = nodeInfo.isVisible,
                resourceId = nodeInfo.viewIdResourceName,
                packageName = nodeInfo.packageName?.toString(),
                parent = parent
            )
            
            nodes.add(treeNode)
            
            // Recursively process children
            for (i in 0 until nodeInfo.childCount) {
                val child = nodeInfo.getChild(i)
                if (child != null) {
                    nodes.addAll(fromNodeInfo(child, treeNode))
                    child.recycle()
                }
            }
            
            return nodes
        }
    }
}
