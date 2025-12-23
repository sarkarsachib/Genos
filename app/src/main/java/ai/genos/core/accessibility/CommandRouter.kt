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
     * Dispatches the given accessibility command to the appropriate handler and returns the execution result.
     *
     * @param command The accessibility command containing a command type and any parameters required by that command.
     * @param service The GenosAccessibilityService used to perform accessibility operations and query state.
     * @return A CommandResult describing the outcome. On success the result contains any command-specific data and timestamp; on failure `success` is `false` and `error` contains a message. */
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
    
    /**
     * Obtain the current UI tree snapshot from the service and wrap it in a CommandResult.
     *
     * @return A CommandResult whose `success` is true when a snapshot was obtained, `data` contains the snapshot (or `null`), and `timestamp` is the current system time in milliseconds.
     */
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
    
    /**
     * Retrieve the current accessibility context from the service.
     *
     * @return A CommandResult with `success = true`, `data` set to the current accessibility context returned by the service, and a `timestamp` of when the result was produced.
     */
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
    
    /**
     * Fetches recent accessibility transitions and returns them as a command result.
     *
     * Reads an optional `limit` parameter from `command.parameters` (defaults to 10) and requests
     * that many recent transitions from the provided service.
     *
     * @param command The accessibility command whose `parameters` may include an integer `limit`.
     * @param service The accessibility service used to obtain recent transitions.
     * @return A [CommandResult] whose `data` is the list of recent transitions, `success` is `true`,
     * and `timestamp` is the current system time in milliseconds.
     */
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
    
    /**
     * Executes an accessibility action on the node identified by `nodeId` in the provided command.
     *
     * @param command Command containing `nodeId` (String), `action` (Int), and optional `arguments` (android.os.Bundle) used for the action.
     * @param service Service used to obtain the active window root and perform node extraction/actions.
     * @return CommandResult indicating whether the action was performed. On failure, `success` is `false` and `error` contains a message such as "Missing nodeId or action parameter", "No active window", "Node not found: <nodeId>", or the exception message.
     */
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
    
    /**
     * Searches the active window's accessibility node tree for nodes whose text or content description
     * contains the provided search text (case-insensitive) and returns their UI representations.
     *
     * @param command Contains a "text" parameter with the search string (defaults to empty string).
     * @param service Accessibility service used to obtain the active window root and perform traversal.
     * @return `CommandResult` whose `data` is a list of matching `UiNode` objects when found and `success` is `true`; `success` is `false` and `error` contains a message if no active window is available or an exception occurs.
     */
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
    
    /**
     * Finds a node by its computed ID and returns its tree representation.
     *
     * Looks for a "nodeId" entry in command.parameters, searches the active window's
     * accessibility node tree for a matching node, and returns the extracted node
     * tree when found. If there is no active window or the node cannot be found,
     * the result contains an error message.
     *
     * @param command Command containing parameters; expects "nodeId" (String) to identify the target node.
     * @param service Accessibility service used to access the active window and extract node trees.
     * @return A CommandResult whose `data` is the extracted node tree when `success` is `true`; when `success` is `false` contains an `error` message and a `timestamp`. */
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
    
    /**
     * Scrolls a node identified by its computed ID in the specified direction.
     *
     * Expects `command.parameters` to contain:
     * - `"nodeId"` (String): the computed ID of the target node (required).
     * - `"direction"` (String): either `"forward"` or `"backward"` (optional, defaults to `"forward"`).
     *
     * @param command Command containing the parameters described above.
     * @param service Accessibility service used to obtain the active window root and perform actions.
     * @return `CommandResult` with `success = true` if the scroll action was performed, `success = false` and an `error` message otherwise; `timestamp` is when the result was produced. Error messages include, for example, "Missing nodeId parameter", "No active window", or "Node not found: <nodeId>".
     */
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
    
    /**
     * Performs a click action on the accessibility node identified by `nodeId` in the given command.
     *
     * Attempts to locate the node under the service's active window and invoke ACTION_CLICK. The command
     * must include a string parameter `nodeId`. On failure the result's `error` contains a short message
     * (e.g., missing parameter, no active window, node not found, or exception message).
     *
     * @param command Accessibility command whose `parameters["nodeId"]` specifies the target node ID.
     * @param service Service used to access the active window and node tree.
     * @return `true` if the click action was performed successfully, `false` otherwise.
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
    
    /**
     * Sets the provided text on the accessibility node identified by `nodeId`.
     *
     * Attempts to find the target node in the active window, performs ACTION_SET_TEXT with the
     * provided text, and returns a result indicating whether the action succeeded.
     *
     * @param command The command containing parameters:
     *  - `"nodeId"` (String, required): computed ID of the target node.
     *  - `"text"` (String, optional): text to set (defaults to empty string).
     * @param service The accessibility service used to access the active window and node tree.
     * @return `CommandResult` with `success = true` if the text was applied, `success = false` otherwise.
     *         When unsuccessful, `error` contains a concise message (e.g., missing `nodeId`, no active window,
     *         or node not found) and `timestamp` reflects the result time.
     */
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
    
    /**
     * Locates an AccessibilityNodeInfo within the given node subtree whose computed ID matches `targetId`.
     *
     * @param node The root AccessibilityNodeInfo to search.
     * @param targetId The computed node identifier to match, formatted as `"<className>_<viewIdResourceName>_<hashCode()>"`.
     * @return The matching AccessibilityNodeInfo if found, `null` otherwise.
     */
    
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
    
    /**
     * Recursively searches the accessibility node subtree for nodes whose text or contentDescription contains the given text (case-insensitive).
     *
     * @param node The root AccessibilityNodeInfo to search through.
     * @param text The substring to match against node text and contentDescription.
     * @return A list of UiNode representations for nodes that match the search text (case-insensitive). An empty list if no matches are found.
     */
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