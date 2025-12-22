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
         * Get the singleton instance of the service manager
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
    
    override fun onInterrupt() {
        Logger.logInfo(TAG, "Service interrupted")
        isServiceStarted.set(false)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Logger.logInfo(TAG, "Service destroyed")
        isServiceStarted.set(false)
        instance = null
    }
    
    // UI Tree Extraction Methods
    
    /**
     * Extract the complete UI tree for the current screen
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
     * Extract UI tree for a specific node
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
     * Find node by accessibility ID
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
     * Recursively extract node tree structure
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
     * Extract children nodes
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
     * Extract node bounds
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
     * Extract accessibility attributes
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
     * Generate unique node ID
     */
    private fun generateNodeId(node: AccessibilityNodeInfo): String {
        return "${node.className}_${node.viewIdResourceName}_${node.hashCode()}"
    }
    
    // Event Handlers
    
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
    
    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        // Tree structure changed, notify listeners
        val treeSnapshot = getCurrentUiTree()
        treeSnapshot?.let { notifyTreeListeners(it) }
    }
    
    private fun handleViewScrolled(event: AccessibilityEvent) {
        // Scroll event detected
        Logger.logDebug(TAG, "Scroll event in ${event.packageName}")
    }
    
    private fun handleViewClicked(event: AccessibilityEvent) {
        // Click event detected
        Logger.logDebug(TAG, "Click event in ${event.packageName}")
    }
    
    private fun handleViewFocused(event: AccessibilityEvent) {
        // Focus event detected
        Logger.logDebug(TAG, "Focus event in ${event.packageName}")
    }
    
    private fun handleNotificationStateChanged(event: AccessibilityEvent) {
        // Notification event detected
        Logger.logDebug(TAG, "Notification event in ${event.packageName}")
    }
    
    private fun handleScreenStateChanged(event: AccessibilityEvent) {
        isScreenOn = event.isEnabled
        Logger.logDebug(TAG, "Screen state changed: $isScreenOn")
    }
    
    // Event Notification Methods
    
    private fun notifyEventListeners(event: AccessibilityEvent) {
        eventListeners.forEach { listener ->
            try {
                listener.onAccessibilityEvent(event)
            } catch (e: Exception) {
                Logger.logError(TAG, "Error notifying event listener", e)
            }
        }
    }
    
    private fun notifyContextListeners(transition: AppTransition) {
        contextListeners.forEach { listener ->
            try {
                listener.onAppTransition(transition)
            } catch (e: Exception) {
                Logger.logError(TAG, "Error notifying context listener", e)
            }
        }
    }
    
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
     * Add event listener
     */
    fun addEventListener(listener: AccessibilityEventListener) {
        eventListeners.add(listener)
    }
    
    /**
     * Remove event listener
     */
    fun removeEventListener(listener: AccessibilityEventListener) {
        eventListeners.remove(listener)
    }
    
    /**
     * Add context listener
     */
    fun addContextListener(listener: AppContextListener) {
        contextListeners.add(listener)
    }
    
    /**
     * Remove context listener
     */
    fun removeContextListener(listener: AppContextListener) {
        contextListeners.remove(listener)
    }
    
    /**
     * Add tree listener
     */
    fun addTreeListener(listener: UiTreeListener) {
        treeListeners.add(listener)
    }
    
    /**
     * Remove tree listener
     */
    fun removeTreeListener(listener: UiTreeListener) {
        treeListeners.remove(listener)
    }
    
    /**
     * Get current app context
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
     * Execute command through router
     */
    fun executeCommand(command: AccessibilityCommand): CommandResult {
        return commandRouter.execute(command, this)
    }
    
    /**
     * Get recent transitions
     */
    fun getRecentTransitions(limit: Int = 10): List<AppTransition> {
        return getServiceManager().getRecentTransitions(limit)
    }
    
    /**
     * Start foreground service with notification
     */
    private fun startForegroundService() {
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
    }
    
    /**
     * Create foreground service notification
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