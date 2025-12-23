package ai.genos.core.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import android.graphics.Rect
import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
import android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS
import android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
import android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
import android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Command router for executing accessibility commands
 */
class CommandRouter {
    
    companion object {
        private const val TAG = "CommandRouter"
    }
    
    /**
     * Execute an accessibility command
     */
    suspend fun execute(command: AccessibilityCommand, service: GenosAccessibilityService): CommandResult {
        return try {
            when (command.type) {
                CommandType.GET_TREE_SNAPSHOT -> executeGetTreeSnapshot(command, service)
                CommandType.GET_CURRENT_CONTEXT -> executeGetCurrentContext(command, service)
                CommandType.GET_RECENT_TRANSITIONS -> executeGetRecentTransitions(command, service)
                CommandType.EXECUTE_ACTION -> executeAction(command, service)
                CommandType.FIND_NODE_BY_TEXT -> executeFindNodeByText(command, service)
                CommandType.FIND_NODE_BY_ID -> executeFindNodeById(command, service)
                CommandType.SCROLL_NODE -> executeScrollNode(command, service)
                CommandType.CLICK_NODE -> executeClickNode(command, service)
                CommandType.SET_TEXT -> executeSetText(command, service)
            }
        } catch (e: Exception) {
            Logger.logError(TAG, "Error executing command: ${command.type}", e)
            CommandResult(
                success = false,
                error = e.message,
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    private suspend fun executeGetTreeSnapshot(
        command: AccessibilityCommand, 
        service: GenosAccessibilityService
    ): CommandResult {
        return suspendCoroutine { continuation ->
            val treeSnapshot = service.getCurrentUiTree()
            continuation.resume(
                CommandResult(
                    success = treeSnapshot != null,
                    data = treeSnapshot,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    private suspend fun executeGetCurrentContext(
        command: AccessibilityCommand, 
        service: GenosAccessibilityService
    ): CommandResult {
        return suspendCoroutine { continuation ->
            val context = service.getCurrentContext()
            continuation.resume(
                CommandResult(
                    success = true,
                    data = context,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    private suspend fun executeGetRecentTransitions(
        command: AccessibilityCommand, 
        service: GenosAccessibilityService
    ): CommandResult {
        return suspendCoroutine { continuation ->
            val limit = command.parameters["limit"] as? Int ?: 10
            val transitions = service.getRecentTransitions(limit)
            continuation.resume(
                CommandResult(
                    success = true,
                    data = transitions,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    private suspend fun executeAction(
        command: AccessibilityCommand, 
        service: GenosAccessibilityService
    ): CommandResult {
        return suspendCoroutine { continuation ->
            try {
                val nodeId = command.parameters["nodeId"] as? String
                val action = command.parameters["action"] as? Int
                
                if (nodeId == null || action == null) {
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "Missing nodeId or action parameter",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    return@suspendCoroutine
                }
                
                val rootNode = service.rootInActiveWindow
                if (rootNode == null) {
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "No active window",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    return@suspendCoroutine
                }
                
                val targetNode = findNodeByIdRecursive(rootNode, nodeId)
                if (targetNode == null) {
                    rootNode.recycle()
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "Node not found: $nodeId",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    return@suspendCoroutine
                }
                
                val arguments = command.parameters["arguments"] as? android.os.Bundle
                val success = targetNode.performAction(action, arguments)
                targetNode.recycle()
                rootNode.recycle()
                
                continuation.resume(
                    CommandResult(
                        success = success,
                        timestamp = System.currentTimeMillis()
                    )
                )
                
            } catch (e: Exception) {
                continuation.resume(
                    CommandResult(
                        success = false,
                        error = e.message,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    private suspend fun executeFindNodeByText(
        command: AccessibilityCommand, 
        service: GenosAccessibilityService
    ): CommandResult {
        return suspendCoroutine { continuation ->
            try {
                val text = command.parameters["text"] as? String ?: ""
                val rootNode = service.rootInActiveWindow
                
                if (rootNode == null) {
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "No active window",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    return@suspendCoroutine
                }
                
                val foundNodes = findNodesByTextRecursive(rootNode, text)
                rootNode.recycle()
                
                continuation.resume(
                    CommandResult(
                        success = true,
                        data = foundNodes,
                        timestamp = System.currentTimeMillis()
                    )
                )
                
            } catch (e: Exception) {
                continuation.resume(
                    CommandResult(
                        success = false,
                        error = e.message,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    private suspend fun executeFindNodeById(
        command: AccessibilityCommand, 
        service: GenosAccessibilityService
    ): CommandResult {
        return suspendCoroutine { continuation ->
            try {
                val nodeId = command.parameters["nodeId"] as? String ?: ""
                val rootNode = service.rootInActiveWindow
                
                if (rootNode == null) {
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "No active window",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    return@suspendCoroutine
                }
                
                val foundNode = findNodeByIdRecursive(rootNode, nodeId)
                rootNode.recycle()
                
                if (foundNode != null) {
                    val treeNode = service.extractNodeTree(foundNode)
                    foundNode.recycle()
                    
                    continuation.resume(
                        CommandResult(
                            success = true,
                            data = treeNode,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } else {
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "Node not found: $nodeId",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                
            } catch (e: Exception) {
                continuation.resume(
                    CommandResult(
                        success = false,
                        error = e.message,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    private suspend fun executeScrollNode(
        command: AccessibilityCommand, 
        service: GenosAccessibilityService
    ): CommandResult {
        return suspendCoroutine { continuation ->
            try {
                val nodeId = command.parameters["nodeId"] as? String
                val direction = command.parameters["direction"] as? String ?: "forward"
                
                if (nodeId == null) {
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "Missing nodeId parameter",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    return@suspendCoroutine
                }
                
                val rootNode = service.rootInActiveWindow
                if (rootNode == null) {
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "No active window",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    return@suspendCoroutine
                }
                
                val targetNode = findNodeByIdRecursive(rootNode, nodeId)
                if (targetNode == null) {
                    rootNode.recycle()
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "Node not found: $nodeId",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    return@suspendCoroutine
                }
                
                val scrollAction = if (direction == "backward") ACTION_SCROLL_BACKWARD else ACTION_SCROLL_FORWARD
                val success = targetNode.performAction(scrollAction)
                targetNode.recycle()
                rootNode.recycle()
                
                continuation.resume(
                    CommandResult(
                        success = success,
                        timestamp = System.currentTimeMillis()
                    )
                )
                
            } catch (e: Exception) {
                continuation.resume(
                    CommandResult(
                        success = false,
                        error = e.message,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    private suspend fun executeClickNode(
        command: AccessibilityCommand, 
        service: GenosAccessibilityService
    ): CommandResult {
        return suspendCoroutine { continuation ->
            try {
                val nodeId = command.parameters["nodeId"] as? String
                
                if (nodeId == null) {
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "Missing nodeId parameter",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    return@suspendCoroutine
                }
                
                val rootNode = service.rootInActiveWindow
                if (rootNode == null) {
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "No active window",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    return@suspendCoroutine
                }
                
                val targetNode = findNodeByIdRecursive(rootNode, nodeId)
                if (targetNode == null) {
                    rootNode.recycle()
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "Node not found: $nodeId",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    return@suspendCoroutine
                }
                
                val success = targetNode.performAction(ACTION_CLICK)
                targetNode.recycle()
                rootNode.recycle()
                
                continuation.resume(
                    CommandResult(
                        success = success,
                        timestamp = System.currentTimeMillis()
                    )
                )
                
            } catch (e: Exception) {
                continuation.resume(
                    CommandResult(
                        success = false,
                        error = e.message,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    private suspend fun executeSetText(
        command: AccessibilityCommand, 
        service: GenosAccessibilityService
    ): CommandResult {
        return suspendCoroutine { continuation ->
            try {
                val nodeId = command.parameters["nodeId"] as? String
                val text = command.parameters["text"] as? String ?: ""
                
                if (nodeId == null) {
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "Missing nodeId parameter",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    return@suspendCoroutine
                }
                
                val rootNode = service.rootInActiveWindow
                if (rootNode == null) {
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "No active window",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    return@suspendCoroutine
                }
                
                val targetNode = findNodeByIdRecursive(rootNode, nodeId)
                if (targetNode == null) {
                    rootNode.recycle()
                    continuation.resume(
                        CommandResult(
                            success = false,
                            error = "Node not found: $nodeId",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    return@suspendCoroutine
                }
                
                val arguments = android.os.Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                val success = targetNode.performAction(ACTION_SET_TEXT, arguments)
                targetNode.recycle()
                rootNode.recycle()
                
                continuation.resume(
                    CommandResult(
                        success = success,
                        timestamp = System.currentTimeMillis()
                    )
                )
                
            } catch (e: Exception) {
                continuation.resume(
                    CommandResult(
                        success = false,
                        error = e.message,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    // Helper methods
    
    private fun findNodeByIdRecursive(node: AccessibilityNodeInfo, targetId: String): AccessibilityNodeInfo? {
        val nodeId = "${node.className}_${node.viewIdResourceName}_${node.hashCode()}"
        if (nodeId == targetId) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByIdRecursive(child, targetId)
            if (found != null) {
                return found
            }
        }
        
        return null
    }
    
    private fun findNodesByTextRecursive(node: AccessibilityNodeInfo, text: String): List<UiNode> {
        val foundNodes = mutableListOf<UiNode>()
        
        val nodeText = node.text?.toString() ?: ""
        val contentDescription = node.contentDescription?.toString() ?: ""
        
        if (nodeText.contains(text, ignoreCase = true) || 
            contentDescription.contains(text, ignoreCase = true)) {
            foundNodes.add(UiNode(
                nodeId = "${node.className}_${node.viewIdResourceName}_${node.hashCode()}",
                className = node.className?.toString() ?: "",
                resourceName = node.viewIdResourceName?.toString() ?: "",
                contentDescription = contentDescription,
                text = nodeText,
                isClickable = node.isClickable,
                isFocusable = node.isFocusable,
                isEnabled = node.isEnabled,
                isVisible = node.isVisibleToUser,
                bounds = null, // Will be calculated if needed
                accessibilityAttributes = emptyMap(),
                children = emptyList()
            ))
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            foundNodes.addAll(findNodesByTextRecursive(child, text))
        }
        
        return foundNodes
    }
}