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
         * Returns the current singleton instance of the accessibility service.
         *
         * @return The initialized GenosAccessibilityService instance, or `null` if the service is not started.
         */
        @Synchronized
        fun getInstance(): GenosAccessibilityService? {
            return instance
        }
        
        /**
         * Provide the singleton AccessibilityServiceManager, creating and storing a new instance if one does not exist.
         *
         * @return The existing singleton AccessibilityServiceManager, or a newly created instance if none was present.
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
     * Initializes the accessibility service when the system connects it.
     *
     * Configures the service's event monitoring and feedback settings, starts the service in the foreground,
     * records the running state, initializes the service manager, and stores a singleton reference to this service.
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
     * Dispatches incoming accessibility events to the appropriate internal handlers and notifies registered listeners.
     *
     * If `event` is null it is ignored. Exceptions thrown during processing are caught and logged.
     *
     * @param event The accessibility event to process; nullable events are ignored.
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
     * Handles the accessibility service being interrupted.
     *
     * Marks the internal started flag as false so callers observe the service as stopped.
     */
    override fun onInterrupt() {
        Logger.logInfo(TAG, "Service interrupted")
        isServiceStarted.set(false)
    }
    
    /**
     * Cleans up runtime state when the accessibility service is destroyed.
     *
     * Clears the singleton instance and marks the service as not started.
     */
    override fun onDestroy() {
        super.onDestroy()
        Logger.logInfo(TAG, "Service destroyed")
        isServiceStarted.set(false)
        instance = null
    }
    
    // UI Tree Extraction Methods
    
    /**
     * Builds a snapshot of the current active window's UI tree and surrounding app context.
     *
     * The snapshot contains a timestamp, package name, activity name, window title, and the root UI node.
     * Returns `null` if there is no active window or if an error occurs during extraction.
     *
     * @return The constructed [UiTreeSnapshot], or `null` when unavailable or on error.
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
     * Builds a UI tree snapshot rooted at the view matching the given resource id.
     *
     * @param nodeId The numeric view resource id to locate (matches the id portion of a node's viewIdResourceName).
     * @return A UiTreeSnapshot whose root is the matched node's subtree, or `null` if the node is not found or an error occurs.
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
     * Searches the given node subtree for an accessibility node whose `viewIdResourceName`
     * contains the pattern "id/{targetId}" and returns the first match.
     *
     * @param node The root node of the subtree to search.
     * @param targetId The numeric id to match against a node's `viewIdResourceName`.
     * @return The matching `AccessibilityNodeInfo` if found, `null` otherwise.
     *
     * Note: This function recycles traversed child nodes but does not recycle the node it returns;
     * the caller is responsible for recycling the returned `AccessibilityNodeInfo`.
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
     * Builds a UiNode representation for the given AccessibilityNodeInfo and its descendants.
     *
     * @param node The root AccessibilityNodeInfo to convert into a UiNode. Caller retains responsibility for recycling the node.
     * @return A UiNode representing the provided node and its full child subtree.
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
     * Extracts the child UI nodes of the provided accessibility node.
     *
     * @return A list of UiNode objects representing each child subtree in the same order as the accessibility node's children.
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
     * Obtain the screen bounds of the given accessibility node as a NodeBounds object.
     *
     * @param node The AccessibilityNodeInfo whose bounds will be read.
     * @return A NodeBounds containing left, top, right, bottom, width, and height, or `null` if the bounds cannot be determined.
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
     * Builds a map of accessibility-related attributes extracted from the given node.
     *
     * Includes editable, password, scrollable and selected flags; role description, hint text, and error text;
     * API-level stateDescription and tooltipText when available; and `extras` as a map of keys to string values on supported Android versions.
     *
     * @param node The AccessibilityNodeInfo to extract attributes from.
     * @return A map where keys are attribute names and values are their extracted values. Text attributes are empty strings when unavailable and `extras` is an empty map when not present or unsupported on the device API level.
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
     * Create a unique identifier for the given accessibility node.
     *
     * @param node The AccessibilityNodeInfo to identify.
     * @return A string composed of the node's class name, view resource name, and hash code. 
     */
    private fun generateNodeId(node: AccessibilityNodeInfo): String {
        return "${node.className}_${node.viewIdResourceName}_${node.hashCode()}"
    }
    
    /**
     * Handles window state change events by updating the service's current app context and emitting a transition when the package or activity changes.
     *
     * When a change is detected, an AppTransition containing timestamp, previous and current package/activity names, and the event type is created and dispatched to registered context listeners.
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
     * Handle a window content change by capturing the current UI tree and dispatching it to registered tree listeners.
     *
     * @param event The accessibility event describing the window content change.
     */
    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        // Tree structure changed, notify listeners
        val treeSnapshot = getCurrentUiTree()
        treeSnapshot?.let { notifyTreeListeners(it) }
    }
    
    /**
     * Handles an accessibility view-scrolled event and logs its originating package name for diagnostics.
     *
     * @param event The AccessibilityEvent representing the scroll event. */
    private fun handleViewScrolled(event: AccessibilityEvent) {
        // Scroll event detected
        Logger.logDebug(TAG, "Scroll event in ${event.packageName}")
    }
    
    /**
     * Handles an accessibility view click event and records the occurrence.
     *
     * @param event The AccessibilityEvent representing the click.
    private fun handleViewClicked(event: AccessibilityEvent) {
        // Click event detected
        Logger.logDebug(TAG, "Click event in ${event.packageName}")
    }
    
    /**
     * Handles an accessibility view focus event.
     *
     * @param event The AccessibilityEvent that describes the focus change. 
     */
    private fun handleViewFocused(event: AccessibilityEvent) {
        // Focus event detected
        Logger.logDebug(TAG, "Focus event in ${event.packageName}")
    }
    
    /**
     * Handles accessibility events of type TYPE_NOTIFICATION_STATE_CHANGED by recording the notification event.
     *
     * Logs the originating package name for the notification event.
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
     * @param event The accessibility event carrying the new screen state; `event.isEnabled` is used to set `isScreenOn`.
     */
    private fun handleScreenStateChanged(event: AccessibilityEvent) {
        isScreenOn = event.isEnabled
        Logger.logDebug(TAG, "Screen state changed: $isScreenOn")
    }
    
    /**
     * Dispatches an AccessibilityEvent to all registered event listeners.
     *
     * Iterates through the internal listener list and invokes `onAccessibilityEvent` on each
     * listener; exceptions thrown by individual listeners are caught and logged so delivery
     * to other listeners continues.
     *
     * @param event The accessibility event to deliver to listeners.
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
     * Notifies all registered app context listeners of the given application transition.
     *
     * @param transition Details of the app transition including timestamps, from/to package and activity names, and the triggering event type.
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
     * Notifies all registered UI tree listeners of an updated UI tree snapshot.
     *
     * Exceptions thrown by individual listeners are caught and logged so that a failure
     * in one listener does not prevent notifications from reaching others.
     *
     * @param snapshot The UI tree snapshot to deliver to listeners.
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
     * Registers an AccessibilityEventListener to receive dispatched accessibility events from this service.
     *
     * @param listener The listener to be notified of accessibility events.
     */
    fun addEventListener(listener: AccessibilityEventListener) {
        eventListeners.add(listener)
    }
    
    /**
     * Unregisters an accessibility event listener so it no longer receives events.
     *
     * @param listener The listener to remove.
     */
    fun removeEventListener(listener: AccessibilityEventListener) {
        eventListeners.remove(listener)
    }
    
    /**
     * Registers an AppContextListener to receive app context transition notifications.
     *
     * @param listener Listener that will be notified when the current app package, activity, or window title changes.
     */
    fun addContextListener(listener: AppContextListener) {
        contextListeners.add(listener)
    }
    
    /**
     * Removes a previously registered app context listener.
     *
     * @param listener The AppContextListener to unregister.
     */
    fun removeContextListener(listener: AppContextListener) {
        contextListeners.remove(listener)
    }
    
    /**
     * Registers a listener to receive notifications when the UI tree changes.
     *
     * @param listener The UiTreeListener invoked whenever the service produces a new or updated UI tree snapshot.
     */
    fun addTreeListener(listener: UiTreeListener) {
        treeListeners.add(listener)
    }
    
    /**
     * Unregisters a UI tree listener so it no longer receives UI tree change notifications.
     *
     * @param listener The UiTreeListener to remove.
     */
    fun removeTreeListener(listener: UiTreeListener) {
        treeListeners.remove(listener)
    }
    
    /**
     * Retrieve the current application context snapshot including package, activity, window title, and screen state.
     *
     * @return An AppContext containing the current packageName, activityName, windowTitle, isScreenOn flag, and the current timestamp.
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
     * Routes an accessibility command to the service's command router.
     *
     * @param command The accessibility command to execute.
     * @return The result of executing the command.
     */
    fun executeCommand(command: AccessibilityCommand): CommandResult {
        return commandRouter.execute(command, this)
    }
    
    /**
     * Retrieve recent app transitions observed by the service.
     *
     * @param limit Maximum number of transitions to return (default 10).
     * @return A list of AppTransition objects ordered from newest to oldest.
     */
    fun getRecentTransitions(limit: Int = 10): List<AppTransition> {
        return getServiceManager().getRecentTransitions(limit)
    }
    
    /**
     * Start the accessibility service as a foreground service and post a persistent notification.
     */
    private fun startForegroundService() {
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
    }
    
    /**
     * Creates and posts the persistent notification used for the service's foreground presence.
     *
     * Creates a notification channel on Android O and above, builds an ongoing low-priority
     * service notification (title, text, and icon), and publishes it via NotificationManagerCompat.
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