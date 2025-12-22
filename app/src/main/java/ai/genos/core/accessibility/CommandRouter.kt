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
     * Dispatches an AccessibilityCommand to the appropriate handler and reports the execution outcome.
     *
     * If a handler throws an exception, the function returns a failure result containing the exception message and the current timestamp.
     *
     * @returns A CommandResult describing whether execution succeeded, any returned data, an error message when applicable, and a timestamp. 
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
    
    /**
     * Retrieves the current UI tree snapshot from the accessibility service.
     *
     * @return A CommandResult whose `success` is `true` if a snapshot was obtained, `false` otherwise.
     *         `data` contains the snapshot when present, and `timestamp` is set to the current time.
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
     * Retrieve the current accessibility context from the provided service.
     *
     * @return `CommandResult` containing the current context in `data`, `success = true`, and a timestamp.
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
     * Fetches recent UI transitions and returns them wrapped in a CommandResult.
     *
     * The maximum number of transitions returned is taken from `command.parameters["limit"]`,
     * defaulting to 10 when the parameter is absent or not an integer.
     *
     * @return A CommandResult with `success` set to `true`, `data` containing the list of recent transitions,
     * and `timestamp` set to the current time in milliseconds.
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
     * Performs the specified accessibility action on the node identified by `nodeId` in the command.
     *
     * @param command AccessibilityCommand containing:
     *  - `nodeId` (String): identifier of the target node,
     *  - `action` (Int): the accessibility action constant to perform,
     *  - optional `arguments` (android.os.Bundle) to pass to the action.
     * @return `CommandResult` with `success` set to `true` if the action was performed, `false` and an `error` message otherwise; `timestamp` reflects the operation time.
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
     * Finds accessibility nodes whose text or content description contains the provided search text.
     *
     * @param command AccessibilityCommand whose parameters may include a "text" String used for case-insensitive matching (defaults to empty string).
     * @return `true` if the search completed successfully, with `data` containing a list of matching `UiNode` objects; `false` otherwise and `error` set with a message (for example when there is no active window or an exception occurs).
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
     * Finds an accessibility node by its computed ID and returns its extracted tree representation.
     *
     * Expects `command.parameters["nodeId"]` to contain the target node ID. Returns a CommandResult with `data` set to the extracted node tree on success.
     *
     * @param command Contains parameters; `nodeId` (String) is required to identify the target node.
     * @param service Provides access to the active window root and node extraction utilities.
     * @return A CommandResult where `success` is `true` and `data` holds the extracted node tree when the node is found;
     *         `success` is `false` and `error` contains one of:
     *         - "No active window" if there is no root node,
     *         - "Node not found: <nodeId>" if the node cannot be located,
     *         - or the exception message if an unexpected error occurs. */
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
     * Scrolls the accessibility node identified by `nodeId` in the specified direction.
     *
     * The `command` must provide a `nodeId` parameter (String). An optional `direction`
     * parameter may be provided with value `"forward"` (default) or `"backward"`.
     *
     * @param command Contains parameters `nodeId` and optional `direction`.
     * @return `CommandResult` with `success` set to `true` if the scroll action succeeded, `false` otherwise.
     *         On failure `error` contains a descriptive message (e.g., missing `nodeId`, no active window, or node not found)
     *         and `timestamp` records the result time.
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
     * Finds the accessibility node identified by `nodeId` in the given `command` and performs a click action on it.
     *
     * @param command AccessibilityCommand containing a `"nodeId"` parameter (String) that identifies the target node.
     * @return A CommandResult whose `success` is `true` if the click action succeeded, `false` otherwise. When unsuccessful, the `error` contains a brief failure reason (e.g., missing `nodeId`, no active window, node not found, or an exception message). The result always includes a `timestamp`.
     */
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
     * Sets the text on an accessibility node identified by a nodeId in the command.
     *
     * Expects command.parameters to contain:
     * - "nodeId": String identifying the target node.
     * - "text": String to set (defaults to empty string if absent).
     *
     * The function returns a failure CommandResult with an explanatory `error` when:
     * - "nodeId" is missing from parameters,
     * - there is no active window,
     * - the target node cannot be found,
     * - or an exception occurs during execution.
     *
     * @return `CommandResult` with `success = true` if the ACTION_SET_TEXT request was performed successfully; otherwise `success = false` and `error` contains a human-readable message. The result always includes a `timestamp`.
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
     * Recursively searches the given node and its descendants for a node whose computed id equals `targetId`.
     *
     * @param node The root node to start the search from.
     * @param targetId The target identifier to match. The identifier is expected to be in the form
     * "<className>_<viewIdResourceName>_<hashCode()>".
     * @return The matching AccessibilityNodeInfo if found, or `null` if no matching node exists in the subtree.
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
     * Collects UiNode descriptors from the given accessibility node subtree whose text or content
     * description contains the provided search string (case-insensitive).
     *
     * @param node Root accessibility node to search recursively.
     * @param text Substring to match against node text and content description (case-insensitive).
     * @return A list of UiNode objects for each matching node. Each UiNode includes a computed
     * nodeId in the form "<className>_<viewIdResourceName>_<hashCode()>", class and resource names,
     * text and contentDescription, basic state flags (clickable, focusable, enabled, visible), and
     * placeholder values: `bounds = null`, `accessibilityAttributes = emptyMap()`, `children = emptyList()`.
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