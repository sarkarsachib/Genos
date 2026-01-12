package ai.genos.core.accessibility

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Unit tests for GENOS Accessibility Service core logic
 */
class GenosAccessibilityServiceUnitTest {
    
    private lateinit var serviceManager: AccessibilityServiceManager
    private lateinit var commandRouter: CommandRouter
    
    @Before
    fun setUp() {
        serviceManager = AccessibilityServiceManager()
        commandRouter = CommandRouter()
    }
    
    @Test
    fun testServiceManagerTransitions() {
        // Test initial state
        assertFalse("Service should not be running initially", serviceManager.isServiceRunning())
        assertEquals("Should have no transitions initially", 0, serviceManager.getRecentTransitions().size)
        
        // Record a transition
        val transition = AppTransition(
            timestamp = System.currentTimeMillis(),
            fromPackage = "com.example.oldapp",
            toPackage = "com.example.newapp",
            fromActivity = "OldActivity",
            toActivity = "NewActivity",
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
        
        serviceManager.recordTransition(transition)
        
        // Verify transition was recorded
        val recentTransitions = serviceManager.getRecentTransitions()
        assertEquals("Should have one transition", 1, recentTransitions.size)
        assertEquals("Package should match", "com.example.newapp", recentTransitions[0].toPackage)
        assertTrue("Service should be running", serviceManager.isServiceRunning())
    }
    
    @Test
    fun testServiceManagerStatistics() {
        // Test initial statistics
        var stats = serviceManager.getServiceStatistics()
        assertEquals("Should have 0 transitions initially", 0, stats.totalTransitions)
        assertFalse("Service should not be running initially", stats.isServiceRunning)
        
        // Add some transitions
        repeat(5) { index ->
            serviceManager.recordTransition(
                AppTransition(
                    timestamp = System.currentTimeMillis() + index,
                    fromPackage = "com.example.app$index",
                    toPackage = "com.example.app${index + 1}",
                    fromActivity = "Activity$index",
                    toActivity = "Activity${index + 1}",
                    eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                )
            )
        }
        
        // Verify statistics updated
        stats = serviceManager.getServiceStatistics()
        assertEquals("Should have 5 transitions", 5, stats.totalTransitions)
        assertTrue("Service should be running", stats.isServiceRunning)
        assertTrue("Last heartbeat should be recent", stats.lastHeartbeat > 0)
    }
    
    @Test
    fun testUiTreeModels() {
        // Test UiNode creation
        val bounds = NodeBounds(10, 20, 110, 70, 100, 50)
        val node = UiNode(
            nodeId = "test_node_123",
            className = "android.widget.Button",
            resourceName = "id/test_button",
            contentDescription = "Test Button",
            text = "Click Me",
            isClickable = true,
            isFocusable = false,
            isEnabled = true,
            isVisible = true,
            bounds = bounds,
            accessibilityAttributes = mapOf(
                "isEditable" to false,
                "isPassword" to false,
                "roleDescription" to "Button"
            ),
            children = emptyList()
        )
        
        assertEquals("Node ID should match", "test_node_123", node.nodeId)
        assertEquals("Class name should match", "android.widget.Button", node.className)
        assertTrue("Button should be clickable", node.isClickable)
        assertEquals("Bounds width should be 100", 100, node.bounds?.width)
        assertEquals("Bounds height should be 50", 50, node.bounds?.height)
        assertEquals("Should have 3 accessibility attributes", 3, node.accessibilityAttributes.size)
        
        // Test UiTreeSnapshot
        val treeSnapshot = UiTreeSnapshot(
            timestamp = System.currentTimeMillis(),
            packageName = "com.example.testapp",
            activityName = "com.example.testapp.MainActivity",
            windowTitle = "Test Application",
            rootNode = node
        )
        
        assertEquals("Package name should match", "com.example.testapp", treeSnapshot.packageName)
        assertEquals("Activity name should match", "com.example.testapp.MainActivity", treeSnapshot.activityName)
        assertSame("Root node should be the same", node, treeSnapshot.rootNode)
        assertTrue("Timestamp should be recent", treeSnapshot.timestamp > 0)
    }
    
    @Test
    fun testAppContextAndTransitions() {
        // Test AppContext
        val context = AppContext(
            packageName = "com.example.app",
            activityName = "com.example.app.MainActivity",
            windowTitle = "Main Window",
            isScreenOn = true,
            timestamp = System.currentTimeMillis()
        )
        
        assertEquals("Package name should match", "com.example.app", context.packageName)
        assertEquals("Activity name should match", "com.example.app.MainActivity", context.activityName)
        assertEquals("Window title should match", "Main Window", context.windowTitle)
        assertTrue("Screen should be on", context.isScreenOn)
        assertTrue("Timestamp should be recent", context.timestamp > 0)
        
        // Test AppTransition
        val transition = AppTransition(
            timestamp = System.currentTimeMillis(),
            fromPackage = "com.example.oldapp",
            toPackage = "com.example.newapp",
            fromActivity = "com.example.oldapp.OldActivity",
            toActivity = "com.example.newapp.NewActivity",
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
        
        assertEquals("From package should match", "com.example.oldapp", transition.fromPackage)
        assertEquals("To package should match", "com.example.newapp", transition.toPackage)
        assertEquals("From activity should match", "com.example.oldapp.OldActivity", transition.fromActivity)
        assertEquals("To activity should match", "com.example.newapp.NewActivity", transition.toActivity)
        assertEquals("Event type should match", AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, transition.eventType)
        assertTrue("Timestamp should be recent", transition.timestamp > 0)
    }
    
    @Test
    fun testAccessibilityCommands() {
        // Test GET_TREE_SNAPSHOT command
        val treeCommand = AccessibilityCommand(
            type = CommandType.GET_TREE_SNAPSHOT,
            parameters = mapOf(
                "includeBounds" to true,
                "maxDepth" to 5
            ),
            timeoutMs = 10000
        )
        
        assertEquals("Command type should be GET_TREE_SNAPSHOT", CommandType.GET_TREE_SNAPSHOT, treeCommand.type)
        assertTrue("Should include bounds parameter", treeCommand.parameters.containsKey("includeBounds"))
        assertTrue("Should include max depth parameter", treeCommand.parameters.containsKey("maxDepth"))
        assertEquals("Timeout should be 10000ms", 10000, treeCommand.timeoutMs)
        
        // Test EXECUTE_ACTION command
        val actionCommand = AccessibilityCommand(
            type = CommandType.EXECUTE_ACTION,
            parameters = mapOf(
                "nodeId" to "button_123",
                "action" to AccessibilityNodeInfo.ACTION_CLICK,
                "arguments" to null
            ),
            timeoutMs = 5000
        )
        
        assertEquals("Command type should be EXECUTE_ACTION", CommandType.EXECUTE_ACTION, actionCommand.type)
        assertEquals("Node ID should match", "button_123", actionCommand.parameters["nodeId"])
        assertEquals("Action should match", AccessibilityNodeInfo.ACTION_CLICK, actionCommand.parameters["action"])
        
        // Test FIND_NODE_BY_TEXT command
        val findCommand = AccessibilityCommand(
            type = CommandType.FIND_NODE_BY_TEXT,
            parameters = mapOf("text" to "Login"),
            timeoutMs = 3000
        )
        
        assertEquals("Command type should be FIND_NODE_BY_TEXT", CommandType.FIND_NODE_BY_TEXT, findCommand.type)
        assertEquals("Text should match", "Login", findCommand.parameters["text"])
    }
    
    @Test
    fun testCommandResults() {
        // Test successful result
        val successResult = CommandResult(
            success = true,
            data = UiTreeSnapshot(
                timestamp = System.currentTimeMillis(),
                packageName = "com.example.app",
                activityName = "MainActivity",
                windowTitle = "Test",
                rootNode = UiNode(
                    nodeId = "root",
                    className = "LinearLayout",
                    resourceName = "id/root",
                    contentDescription = "",
                    text = "",
                    isClickable = false,
                    isFocusable = false,
                    isEnabled = true,
                    isVisible = true,
                    bounds = null,
                    accessibilityAttributes = emptyMap(),
                    children = emptyList()
                )
            ),
            error = null,
            timestamp = System.currentTimeMillis()
        )
        
        assertTrue("Result should be successful", successResult.success)
        assertNotNull("Data should not be null", successResult.data)
        assertNull("Error should be null", successResult.error)
        assertTrue("Timestamp should be recent", successResult.timestamp > 0)
        
        // Test failed result
        val failureResult = CommandResult(
            success = false,
            data = null,
            error = "Node not found",
            timestamp = System.currentTimeMillis()
        )
        
        assertFalse("Result should not be successful", failureResult.success)
        assertNull("Data should be null", failureResult.data)
        assertEquals("Error should match", "Node not found", failureResult.error)
        assertTrue("Timestamp should be recent", failureResult.timestamp > 0)
    }
    
    @Test
    fun testEventListenerInterfaces() {
        // Test AccessibilityEventListener
        val eventListener = object : AccessibilityEventListener {
            var eventReceived: AccessibilityEvent? = null
            
            override fun onAccessibilityEvent(event: AccessibilityEvent) {
                eventReceived = event
            }
        }
        
        // Create mock event
        val mockEvent = mock(AccessibilityEvent::class.java)
        `when`(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_CLICKED)
        `when`(mockEvent.packageName).thenReturn("com.example.test")
        
        eventListener.onAccessibilityEvent(mockEvent)
        
        assertNotNull("Event should be received", eventListener.eventReceived)
        assertEquals("Event type should match", AccessibilityEvent.TYPE_VIEW_CLICKED, eventListener.eventReceived?.eventType)
        assertEquals("Package should match", "com.example.test", eventListener.eventReceived?.packageName?.toString())
        
        // Test AppContextListener
        val contextListener = object : AppContextListener {
            var transitionReceived: AppTransition? = null
            
            override fun onAppTransition(transition: AppTransition) {
                transitionReceived = transition
            }
        }
        
        val testTransition = AppTransition(
            timestamp = System.currentTimeMillis(),
            fromPackage = "old.package",
            toPackage = "new.package",
            fromActivity = "OldActivity",
            toActivity = "NewActivity",
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
        
        contextListener.onAppTransition(testTransition)
        
        assertNotNull("Transition should be received", contextListener.transitionReceived)
        assertEquals("From package should match", "old.package", contextListener.transitionReceived?.fromPackage)
        assertEquals("To package should match", "new.package", contextListener.transitionReceived?.toPackage)
        
        // Test UiTreeListener
        val treeListener = object : UiTreeListener {
            var snapshotReceived: UiTreeSnapshot? = null
            
            override fun onTreeChanged(snapshot: UiTreeSnapshot) {
                snapshotReceived = snapshot
            }
        }
        
        val testSnapshot = UiTreeSnapshot(
            timestamp = System.currentTimeMillis(),
            packageName = "com.example.app",
            activityName = "MainActivity",
            windowTitle = "Test Window",
            rootNode = UiNode(
                nodeId = "root",
                className = "LinearLayout",
                resourceName = "id/root",
                contentDescription = "",
                text = "",
                isClickable = false,
                isFocusable = false,
                isEnabled = true,
                isVisible = true,
                bounds = null,
                accessibilityAttributes = emptyMap(),
                children = emptyList()
            )
        )
        
        treeListener.onTreeChanged(testSnapshot)
        
        assertNotNull("Snapshot should be received", treeListener.snapshotReceived)
        assertEquals("Package should match", "com.example.app", treeListener.snapshotReceived?.packageName)
        assertEquals("Root node class should match", "LinearLayout", treeListener.snapshotReceived?.rootNode?.className)
    }
}