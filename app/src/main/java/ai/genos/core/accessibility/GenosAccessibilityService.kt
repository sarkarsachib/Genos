package ai.genos.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Core accessibility service for GENOS platform.
 * 
 * Features:
 * - Foreground service with persistent notification
 * - Real-time UI tree extraction and serialization
 * - App context tracking (package, activity, window changes)
 * - Event bus for inter-module communication
 * - Command router for downstream modules
 * - Transition detection and monitoring
 */
class GenosAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "genos_accessibility_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "GenosAccessibilityService"
        
        // Singleton instance
        @Volatile
        private var instance: GenosAccessibilityService? = null
        
        @Volatile
        private var serviceManager: AccessibilityServiceManager? = null
        
        /**
         * Get the singleton instance of the accessibility service
         */
        @Synchronized
        fun getInstance(): GenosAccessibilityService? {
            return instance
        }
        
        /**
         * Return the singleton AccessibilityServiceManager, creating and caching it if necessary.
         *
         * @return The cached `AccessibilityServiceManager` instance.
         */
        @Synchronized
        fun getServiceManager(): AccessibilityServiceManager {
            return serviceManager ?: AccessibilityServiceManager().also { serviceManager = it }
        }
    }
    
    // Service state management
    private val isServiceStarted = AtomicBoolean(false)
    private val serviceLock = ReentrantReadWriteLock()
    
    // Context tracking
    private var currentPackageName: String = ""
    private var currentActivityName: String = ""
    private var currentWindowTitle: String = ""
    private var isScreenOn = true
    
    // JSON serialization
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()
    
    // Event listeners
    private val eventListeners = CopyOnWriteArrayList<AccessibilityEventListener>()
    private val contextListeners = CopyOnWriteArrayList<AppContextListener>()
    private val treeListeners = CopyOnWriteArrayList<UiTreeListener>()
    
    // Command router
    private val commandRouter = CommandRouter()
    
    /**
     * Initialize the service after the system connects this AccessibilityService.
     *
     * Configures the service's monitoring options (event types, feedback type, flags, notification timeout, and package filter),
     * registers this instance as the singleton, starts the service in the foreground, marks the service as started,
     * initializes the AccessibilityServiceManager, and logs successful initialization.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        // Update singleton instance
        instance = this
        
        // Configure service info
        serviceInfo = serviceInfo.apply {
            // Configure event types to monitor
            eventTypes = AccessibilityEvent.TYPE_VIEW_SCROLLED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED or
                    AccessibilityEvent.TYPE_SCREEN_STATE_CHANGED
            
            // Configure feedback type
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            
            // Configure flags
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            
            // Configure notification timeout
            notificationTimeout = 100
            
            // Set package filtering (null means monitor all packages)
            packageNames = null
        }
        
        // Start as foreground service
        startForegroundService()
        
        // Mark service as started
        isServiceStarted.set(true)
        
        // Initialize service manager
        getServiceManager()
        
        Logger.logInfo(TAG, "GenosAccessibilityService connected and initialized")
    }
    
    /**
     * Dispatches an incoming accessibility event to the appropriate handler and forwards it to registered listeners.
     *
     * Errors raised during processing are caught and logged and do not propagate.
     *
     * @param event The accessibility event to process; if null the call is ignored.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    handleWindowStateChanged(event)
                }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    handleWindowContentChanged(event)
                }
                AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                    handleViewScrolled(event)
                }
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    handleViewClicked(event)
                }
                AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                    handleViewFocused(event)
                }
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                    handleNotificationStateChanged(event)
                }
                AccessibilityEvent.TYPE_SCREEN_STATE_CHANGED -> {
                    handleScreenStateChanged(event)
                }
            }
            
            // Notify event listeners
            notifyEventListeners(event)
            
        } catch (e: Exception) {
            Logger.logError(TAG, "Error processing accessibility event", e)
        }
    }
    
    /**
     * Handles the accessibility service being interrupted by logging the interruption and marking the service as stopped.
     */
    override fun onInterrupt() {
        Logger.logInfo(TAG, "Service interrupted")
        isServiceStarted.set(false)
    }
    
    /**
     * Cleans up service state when the accessibility service is destroyed.
     *
     * Calls the superclass destroy logic, logs the destruction, marks the service as not started,
     * and clears the singleton instance reference.
     */
    override fun onDestroy() {
        super.onDestroy()
        Logger.logInfo(TAG, "Service destroyed")
        isServiceStarted.set(false)
        instance = null
    }
    
    // UI Tree Extraction Methods
    
    /**
     * Builds a snapshot of the current screen's UI hierarchy.
     *
     * The snapshot contains a timestamp, the current package and activity names, the window title,
     * and a serialized root UI node describing the view tree.
     *
     * @return The extracted [UiTreeSnapshot], or `null` if there is no active window or if extraction fails.
     */
    fun getCurrentUiTree(): UiTreeSnapshot? {
        return serviceLock.read {
            try {
                val rootNode = rootInActiveWindow ?: return@read null
                val treeNode = extractNodeTree(rootNode)
                rootNode.recycle()
                
                UiTreeSnapshot(
                    timestamp = System.currentTimeMillis(),
                    packageName = currentPackageName,
                    activityName = currentActivityName,
                    windowTitle = currentWindowTitle,
                    rootNode = treeNode
                )
            } catch (e: Exception) {
                Logger.logError(TAG, "Error extracting UI tree", e)
                null
            }
        }
    }
    
    /**
     * Produce a UI tree snapshot for the subtree rooted at the node identified by `nodeId`.
     *
     * @param nodeId The view node identifier to locate (matches nodes whose resource name contains `id/{nodeId}`).
     * @return A `UiTreeSnapshot` containing the subtree rooted at the matching node, or `null` if the node cannot be found or extraction fails.
     */
    fun getUiTreeForNode(nodeId: Int): UiTreeSnapshot? {
        return serviceLock.read {
            try {
                val rootNode = rootInActiveWindow ?: return@read null
                val targetNode = findNodeById(rootNode, nodeId)
                
                if (targetNode != null) {
                    val treeNode = extractNodeTree(targetNode)
                    targetNode.recycle()
                    rootNode.recycle()
                    
                    UiTreeSnapshot(
                        timestamp = System.currentTimeMillis(),
                        packageName = currentPackageName,
                        activityName = currentActivityName,
                        windowTitle = currentWindowTitle,
                        rootNode = treeNode
                    )
                } else {
                    rootNode.recycle()
                    null
                }
            } catch (e: Exception) {
                Logger.logError(TAG, "Error extracting UI tree for node", e)
                null
            }
        }
    }
    
    /**
     * Searches the subtree rooted at the given node for an accessibility node whose `viewIdResourceName`
     * contains the segment `id/{targetId}`.
     *
     * @param node The root AccessibilityNodeInfo to begin the search from.
     * @param targetId The numeric view id to match within `viewIdResourceName`.
     * @return The matching AccessibilityNodeInfo if found, or `null` if no match exists.
     */
    private fun findNodeById(node: AccessibilityNodeInfo, targetId: Int): AccessibilityNodeInfo? {
        if (node.viewIdResourceName?.contains("id/$targetId") == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeById(child, targetId)
            if (found != null) {
                child.recycle()
                return found
            }
            child.recycle()
        }
        
        return null
    }
    
    /**
     * Builds a UiNode representing the given AccessibilityNodeInfo and its descendant hierarchy.
     *
     * @param node The source AccessibilityNodeInfo to extract (the returned UiNode mirrors this node's properties and children).
     * @return A UiNode containing the node's metadata, accessibility attributes, bounds, and recursively extracted children.
     */
    private fun extractNodeTree(node: AccessibilityNodeInfo): UiNode {
        return UiNode(
            nodeId = generateNodeId(node),
            className = node.className?.toString() ?: "",
            resourceName = node.viewIdResourceName?.toString() ?: "",
            contentDescription = node.contentDescription?.toString() ?: "",
            text = node.text?.toString() ?: "",
            isClickable = node.isClickable,
            isFocusable = node.isFocusable,
            isEnabled = node.isEnabled,
            isVisible = node.isVisibleToUser,
            bounds = extractNodeBounds(node),
            accessibilityAttributes = extractAccessibilityAttributes(node),
            children = extractChildren(node)
        )
    }
    
    /**
     * Builds a list of UiNode objects for the immediate children of the supplied accessibility node.
     *
     * @param node The root AccessibilityNodeInfo whose children will be extracted.
     * @return A list of UiNode representing each extracted child subtree, in child order. Child nodes that are null are skipped.
     *
     * Note: Each child AccessibilityNodeInfo is recycled after its subtree is extracted. 
     */
    private fun extractChildren(node: AccessibilityNodeInfo): List<UiNode> {
        val children = mutableListOf<UiNode>()
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                children.add(extractNodeTree(child))
            } finally {
                child.recycle()
            }
        }
        
        return children
    }
    
    /**
     * Obtain the node's screen bounds as a NodeBounds object.
     *
     * @return A NodeBounds describing the node's bounds in screen coordinates, or `null` if the bounds cannot be determined. 
     */
    private fun extractNodeBounds(node: AccessibilityNodeInfo): NodeBounds? {
        return try {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            NodeBounds(
                left = bounds.left,
                top = bounds.top,
                right = bounds.right,
                bottom = bounds.bottom,
                width = bounds.width(),
                height = bounds.height()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extracts accessibility-related attributes from the given node into a map.
     *
     * The returned map contains observable attribute names and their values:
     * - "isEditable": Boolean
     * - "isPassword": Boolean
     * - "isScrollable": Boolean
     * - "isSelected": Boolean
     * - "roleDescription": String (empty string if not present)
     * - "hintText": String (empty string if not present)
     * - "error": String (empty string if not present)
     * - "stateDescription": String (API 21+, empty string if not present)
     * - "tooltipText": String (API 28+, empty string if not present)
     * - "extras": Map<String, String> (API 29+, empty map if not present)
     *
     * @param node The AccessibilityNodeInfo to extract attributes from.
     * @return A map mapping attribute names to their extracted values.
     */
    private fun extractAccessibilityAttributes(node: AccessibilityNodeInfo): Map<String, Any> {
        val attributes = mutableMapOf<String, Any>()
        
        // Standard attributes
        node.let {
            attributes["isEditable"] = it.isEditable
            attributes["isPassword"] = it.isPassword
            attributes["isScrollable"] = it.isScrollable
            attributes["isSelected"] = it.isSelected
            attributes["roleDescription"] = it.roleDescription?.toString() ?: ""
            attributes["hintText"] = it.hintText?.toString() ?: ""
            attributes["error"] = it.error?.toString() ?: ""
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                attributes["stateDescription"] = it.stateDescription?.toString() ?: ""
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                attributes["tooltipText"] = it.tooltipText?.toString() ?: ""
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                attributes["extras"] = it.extras?.keySet()?.associateWith { key ->
                    it.extras.getString(key) ?: it.extras.getCharSequence(key)?.toString() ?: ""
                } ?: emptyMap()
            }
        }
        
        return attributes
    }
    
    /**
     * Generates a unique identifier string for the given accessibility node.
     *
     * @param node The AccessibilityNodeInfo to identify.
     * @return A string identifier composed from the node's class name, view resource name, and instance hash code.
    private fun generateNodeId(node: AccessibilityNodeInfo): String {
        return "${node.className}_${node.viewIdResourceName}_${node.hashCode()}"
    }
    
    /**
     * Handles a window state change by updating the tracked package and activity and notifying listeners when a transition occurs.
     *
     * @param event The accessibility event representing the window state change.
     */
    
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: ""
        val className = event.className?.toString() ?: ""
        
        // Update context tracking
        val oldPackage = currentPackageName
        val oldActivity = currentActivityName
        
        currentPackageName = packageName
        currentActivityName = className
        
        // Check for transitions
        if (oldPackage != packageName || oldActivity != className) {
            val transition = AppTransition(
                timestamp = System.currentTimeMillis(),
                fromPackage = oldPackage,
                toPackage = packageName,
                fromActivity = oldActivity,
                toActivity = className,
                eventType = event.eventType
            )
            
            Logger.logInfo(TAG, "App transition detected: $transition")
            notifyContextListeners(transition)
        }
    }
    
    /**
     * Handle a window content change event by capturing and dispatching the current UI tree.
     *
     * Attempts to build a snapshot of the active window's UI tree and, if successful, forwards
     * the snapshot to all registered UiTreeListener instances.
     */
    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        // Tree structure changed, notify listeners
        val treeSnapshot = getCurrentUiTree()
        treeSnapshot?.let { notifyTreeListeners(it) }
    }
    
    /**
     * Processes a view-scrolled accessibility event and records its occurrence.
     *
     * @param event The AccessibilityEvent for the view scroll. */
    private fun handleViewScrolled(event: AccessibilityEvent) {
        // Scroll event detected
        Logger.logDebug(TAG, "Scroll event in ${event.packageName}")
    }
    
    /**
     * Handles accessibility events for view click actions.
     *
     * Currently records the package name associated with the click event to the debug log.
     *
     * @param event The accessibility event representing the view click.
     */
    private fun handleViewClicked(event: AccessibilityEvent) {
        // Click event detected
        Logger.logDebug(TAG, "Click event in ${event.packageName}")
    }
    
    /**
     * Processes an accessibility event indicating a view gained focus.
     *
     * @param event The AccessibilityEvent representing the focus change, including package and view details.
     */
    private fun handleViewFocused(event: AccessibilityEvent) {
        // Focus event detected
        Logger.logDebug(TAG, "Focus event in ${event.packageName}")
    }
    
    /**
     * Handles accessibility events that indicate a notification state change.
     *
     * @param event The accessibility event representing the notification state change.
     */
    private fun handleNotificationStateChanged(event: AccessibilityEvent) {
        // Notification event detected
        Logger.logDebug(TAG, "Notification event in ${event.packageName}")
    }
    
    /**
     * Updates the service's screen-on state based on the provided accessibility event.
     *
     * @param event AccessibilityEvent whose `isEnabled` value indicates whether the screen is on. */
    private fun handleScreenStateChanged(event: AccessibilityEvent) {
        isScreenOn = event.isEnabled
        Logger.logDebug(TAG, "Screen state changed: $isScreenOn")
    }
    
    /**
     * Dispatches the given accessibility event to all registered event listeners.
     *
     * Each listener's `onAccessibilityEvent` is invoked; exceptions thrown by a listener
     * are caught and logged so notification continues for remaining listeners.
     *
     * @param event The AccessibilityEvent to forward to listeners.
     */
    
    private fun notifyEventListeners(event: AccessibilityEvent) {
        eventListeners.forEach { listener ->
            try {
                listener.onAccessibilityEvent(event)
            } catch (e: Exception) {
                Logger.logError(TAG, "Error notifying event listener", e)
            }
        }
    }
    
    /**
     * Notifies all registered AppContextListener instances of the provided app transition.
     *
     * Exceptions thrown by individual listeners are caught and logged; notification proceeds for other listeners.
     *
     * @param transition The AppTransition describing the change in package/activity/window context.
     */
    private fun notifyContextListeners(transition: AppTransition) {
        contextListeners.forEach { listener ->
            try {
                listener.onAppTransition(transition)
            } catch (e: Exception) {
                Logger.logError(TAG, "Error notifying context listener", e)
            }
        }
    }
    
    /**
     * Delivers a UI tree snapshot to all registered UiTreeListener instances.
     *
     * Invokes `onTreeChanged` for each listener and logs any exception raised by a listener without interrupting delivery to others.
     *
     * @param snapshot The UI tree snapshot to notify listeners about.
     */
    private fun notifyTreeListeners(snapshot: UiTreeSnapshot) {
        treeListeners.forEach { listener ->
            try {
                listener.onTreeChanged(snapshot)
            } catch (e: Exception) {
                Logger.logError(TAG, "Error notifying tree listener", e)
            }
        }
    }
    
    // Public API Methods
    
    /**
     * Register a listener to receive accessibility events dispatched by the service.
     *
     * @param listener The AccessibilityEventListener to register.
     */
    fun addEventListener(listener: AccessibilityEventListener) {
        eventListeners.add(listener)
    }
    
    /**
     * Unregisters a previously added AccessibilityEventListener so it no longer receives accessibility events.
     *
     * @param listener The listener instance to remove from the service's event listener list.
     */
    fun removeEventListener(listener: AccessibilityEventListener) {
        eventListeners.remove(listener)
    }
    
    /**
     * Registers an AppContextListener to receive future app context transition notifications.
     *
     * @param listener The listener to notify when the current app context or activity changes.
     */
    fun addContextListener(listener: AppContextListener) {
        contextListeners.add(listener)
    }
    
    /**
     * Unregisters an AppContextListener so it no longer receives app context transition notifications.
     *
     * @param listener The listener to remove. If the listener is not registered, this call has no effect.
     */
    fun removeContextListener(listener: AppContextListener) {
        contextListeners.remove(listener)
    }
    
    /**
     * Registers a listener to receive UI tree snapshots when the active window's UI tree changes.
     *
     * The listener will be invoked on each new UiTreeSnapshot produced by the service.
     *
     * @param listener The UiTreeListener to register.
     */
    fun addTreeListener(listener: UiTreeListener) {
        treeListeners.add(listener)
    }
    
    /**
     * Unregisters a UI tree listener so it no longer receives UI tree snapshots.
     *
     * @param listener The listener to remove; if the listener is not registered this is a no-op.
     */
    fun removeTreeListener(listener: UiTreeListener) {
        treeListeners.remove(listener)
    }
    
    /**
     * Provide a snapshot of the current application context.
     *
     * @return An AppContext containing the current package name, activity name, window title,
     *         screen-on state, and a timestamp representing when the snapshot was taken.
     */
    fun getCurrentContext(): AppContext {
        return AppContext(
            packageName = currentPackageName,
            activityName = currentActivityName,
            windowTitle = currentWindowTitle,
            isScreenOn = isScreenOn,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Route an accessibility command through the service's command router.
     *
     * @param command The AccessibilityCommand to execute.
     * @return The resulting CommandResult from executing the command.
     */
    fun executeCommand(command: AccessibilityCommand): CommandResult {
        return commandRouter.execute(command, this)
    }
    
    /**
     * Retrieve recent application transitions.
     *
     * @param limit Maximum number of transitions to return.
     * @return A list of up to `limit` AppTransition records, ordered from newest to oldest.
     */
    fun getRecentTransitions(limit: Int = 10): List<AppTransition> {
        return getServiceManager().getRecentTransitions(limit)
    }
    
    /**
     * Starts the accessibility service in the foreground and displays its persistent notification.
     *
     * This promotes the service to a foreground process and posts the notification returned by
     * createForegroundNotification() using the service's notification ID.
     */
    private fun startForegroundService() {
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
    }
    
    /**
     * Creates and posts the persistent foreground notification used by the accessibility service.
     *
     * On Android O and above this also creates a low-importance notification channel with no sound or badge.
     */
    private fun createForegroundNotification() {
        val channelName = "GENOS Accessibility Service"
        val channelDescription = "Monitors UI interactions and app transitions"
        
        // Create notification channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = channelDescription
                setShowBadge(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create notification
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("GENOS Accessibility Service")
            .setContentText("Monitoring UI interactions and app transitions")
            .setSmallIcon(R.drawable.ic_accessibility_service)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        
        // Show notification
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }
}