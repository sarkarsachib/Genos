package ai.genos.core.accessibility

/**
 * Data classes for UI tree structure
 */

// UI Node representing a single accessibility node
data class UiNode(
    val nodeId: String,
    val className: String,
    val resourceName: String,
    val contentDescription: String,
    val text: String,
    val isClickable: Boolean,
    val isFocusable: Boolean,
    val isEnabled: Boolean,
    val isVisible: Boolean,
    val bounds: NodeBounds?,
    val accessibilityAttributes: Map<String, Any>,
    val children: List<UiNode>
)

// Node bounds information
data class NodeBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val width: Int,
    val height: Int
)

// Complete UI tree snapshot
data class UiTreeSnapshot(
    val timestamp: Long,
    val packageName: String,
    val activityName: String,
    val windowTitle: String,
    val rootNode: UiNode
)

// App context information
data class AppContext(
    val packageName: String,
    val activityName: String,
    val windowTitle: String,
    val isScreenOn: Boolean,
    val timestamp: Long
)

// App transition information
data class AppTransition(
    val timestamp: Long,
    val fromPackage: String,
    val toPackage: String,
    val fromActivity: String,
    val toActivity: String,
    val eventType: Int
)

// Accessibility command types
enum class CommandType {
    GET_TREE_SNAPSHOT,
    GET_CURRENT_CONTEXT,
    GET_RECENT_TRANSITIONS,
    EXECUTE_ACTION,
    FIND_NODE_BY_TEXT,
    FIND_NODE_BY_ID,
    SCROLL_NODE,
    CLICK_NODE,
    SET_TEXT
}

// Accessibility command
data class AccessibilityCommand(
    val type: CommandType,
    val parameters: Map<String, Any> = emptyMap(),
    val timeoutMs: Long = 5000
)

// Command execution result
data class CommandResult(
    val success: Boolean,
    val data: Any? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// Event listener interfaces
interface AccessibilityEventListener {
    fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent)
}

interface AppContextListener {
    fun onAppTransition(transition: AppTransition)
}

interface UiTreeListener {
    fun onTreeChanged(snapshot: UiTreeSnapshot)
}