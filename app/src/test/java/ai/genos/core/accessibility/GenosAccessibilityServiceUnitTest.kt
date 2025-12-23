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
    @Test
    fun testNodeBoundsCreation() {
        val bounds = NodeBounds(10, 20, 110, 70, 100, 50)
        
        assertEquals("Left should be 10", 10, bounds.left)
        assertEquals("Top should be 20", 20, bounds.top)
        assertEquals("Right should be 110", 110, bounds.right)
        assertEquals("Bottom should be 70", 70, bounds.bottom)
        assertEquals("Width should be 100", 100, bounds.width)
        assertEquals("Height should be 50", 50, bounds.height)
    }
    
    @Test
    fun testNodeBoundsCalculations() {
        val bounds1 = NodeBounds(0, 0, 100, 100, 100, 100)
        val bounds2 = NodeBounds(50, 50, 150, 150, 100, 100)
        
        // Test bounds properties
        assertTrue("Bounds should have positive dimensions", bounds1.width > 0 && bounds1.height > 0)
        assertTrue("Bounds should have positive dimensions", bounds2.width > 0 && bounds2.height > 0)
        
        // Test that right = left + width
        assertEquals("Right should equal left + width", bounds1.left + bounds1.width, bounds1.right)
        assertEquals("Right should equal left + width", bounds2.left + bounds2.width, bounds2.right)
        
        // Test that bottom = top + height
        assertEquals("Bottom should equal top + height", bounds1.top + bounds1.height, bounds1.bottom)
        assertEquals("Bottom should equal top + height", bounds2.top + bounds2.height, bounds2.bottom)
    }
    
    @Test
    fun testUiNodeWithChildren() {
        val childNode1 = UiNode(
            nodeId = "child_1",
            className = "android.widget.TextView",
            resourceName = "id/text_view",
            contentDescription = "Text View",
            text = "Hello",
            isClickable = false,
            isFocusable = false,
            isEnabled = true,
            isVisible = true,
            bounds = NodeBounds(10, 10, 110, 60, 100, 50),
            accessibilityAttributes = emptyMap(),
            children = emptyList()
        )
        
        val childNode2 = UiNode(
            nodeId = "child_2",
            className = "android.widget.Button",
            resourceName = "id/button",
            contentDescription = "Submit Button",
            text = "Submit",
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            isVisible = true,
            bounds = NodeBounds(10, 70, 110, 120, 100, 50),
            accessibilityAttributes = mapOf("isEditable" to false),
            children = emptyList()
        )
        
        val parentNode = UiNode(
            nodeId = "parent",
            className = "android.widget.LinearLayout",
            resourceName = "id/container",
            contentDescription = "",
            text = "",
            isClickable = false,
            isFocusable = false,
            isEnabled = true,
            isVisible = true,
            bounds = NodeBounds(0, 0, 120, 130, 120, 130),
            accessibilityAttributes = emptyMap(),
            children = listOf(childNode1, childNode2)
        )
        
        assertEquals("Parent should have 2 children", 2, parentNode.children.size)
        assertEquals("First child should be TextView", "android.widget.TextView", parentNode.children[0].className)
        assertEquals("Second child should be Button", "android.widget.Button", parentNode.children[1].className)
        assertTrue("Second child should be clickable", parentNode.children[1].isClickable)
        assertFalse("First child should not be clickable", parentNode.children[0].isClickable)
    }
    
    @Test
    fun testCommandTypeEnumValues() {
        val allCommandTypes = CommandType.values()
        
        assertTrue("Should have GET_TREE_SNAPSHOT command", allCommandTypes.contains(CommandType.GET_TREE_SNAPSHOT))
        assertTrue("Should have GET_CURRENT_CONTEXT command", allCommandTypes.contains(CommandType.GET_CURRENT_CONTEXT))
        assertTrue("Should have GET_RECENT_TRANSITIONS command", allCommandTypes.contains(CommandType.GET_RECENT_TRANSITIONS))
        assertTrue("Should have EXECUTE_ACTION command", allCommandTypes.contains(CommandType.EXECUTE_ACTION))
        assertTrue("Should have FIND_NODE_BY_TEXT command", allCommandTypes.contains(CommandType.FIND_NODE_BY_TEXT))
        assertTrue("Should have FIND_NODE_BY_ID command", allCommandTypes.contains(CommandType.FIND_NODE_BY_ID))
        assertTrue("Should have SCROLL_NODE command", allCommandTypes.contains(CommandType.SCROLL_NODE))
        assertTrue("Should have CLICK_NODE command", allCommandTypes.contains(CommandType.CLICK_NODE))
        assertTrue("Should have SET_TEXT command", allCommandTypes.contains(CommandType.SET_TEXT))
        
        assertEquals("Should have 9 command types", 9, allCommandTypes.size)
    }
    
    @Test
    fun testAccessibilityCommandCreation() {
        val command = AccessibilityCommand(
            type = CommandType.CLICK_NODE,
            parameters = mapOf("nodeId" to "button_123"),
            timeoutMs = 3000
        )
        
        assertEquals("Type should be CLICK_NODE", CommandType.CLICK_NODE, command.type)
        assertEquals("Node ID parameter should match", "button_123", command.parameters["nodeId"])
        assertEquals("Timeout should be 3000ms", 3000, command.timeoutMs)
    }
    
    @Test
    fun testAccessibilityCommandWithDefaultTimeout() {
        val command = AccessibilityCommand(
            type = CommandType.GET_TREE_SNAPSHOT
        )
        
        assertEquals("Default timeout should be 5000ms", 5000, command.timeoutMs)
        assertTrue("Parameters should be empty", command.parameters.isEmpty())
    }
    
    @Test
    fun testAccessibilityCommandWithMultipleParameters() {
        val params = mapOf(
            "nodeId" to "input_field_456",
            "text" to "Test input",
            "clearExisting" to true,
            "retries" to 3
        )
        
        val command = AccessibilityCommand(
            type = CommandType.SET_TEXT,
            parameters = params,
            timeoutMs = 8000
        )
        
        assertEquals("Should have 4 parameters", 4, command.parameters.size)
        assertEquals("Node ID should match", "input_field_456", command.parameters["nodeId"])
        assertEquals("Text should match", "Test input", command.parameters["text"])
        assertEquals("ClearExisting should be true", true, command.parameters["clearExisting"])
        assertEquals("Retries should be 3", 3, command.parameters["retries"])
    }
    
    @Test
    fun testCommandResultSuccess() {
        val data = mapOf("resultKey" to "resultValue")
        val result = CommandResult(
            success = true,
            data = data,
            error = null,
            timestamp = 123456789L
        )
        
        assertTrue("Result should be successful", result.success)
        assertNotNull("Data should not be null", result.data)
        assertNull("Error should be null", result.error)
        assertEquals("Timestamp should match", 123456789L, result.timestamp)
    }
    
    @Test
    fun testCommandResultFailure() {
        val result = CommandResult(
            success = false,
            data = null,
            error = "Operation failed: Timeout exceeded",
            timestamp = 987654321L
        )
        
        assertFalse("Result should not be successful", result.success)
        assertNull("Data should be null", result.data)
        assertNotNull("Error should not be null", result.error)
        assertEquals("Error message should match", "Operation failed: Timeout exceeded", result.error)
        assertEquals("Timestamp should match", 987654321L, result.timestamp)
    }
    
    @Test
    fun testServiceManagerMaxTransitionHistory() {
        // Add more than MAX_TRANSITION_HISTORY transitions
        repeat(150) { index ->
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
        
        // Should maintain maximum size (typically 100)
        val allTransitions = serviceManager.getAllTransitions()
        assertTrue("Should not exceed max history", allTransitions.size <= 100)
    }
    
    @Test
    fun testServiceManagerClearTransitions() {
        // Add some transitions
        repeat(10) { index ->
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
        
        assertTrue("Should have transitions", serviceManager.getRecentTransitions().isNotEmpty())
        
        // Clear transitions
        serviceManager.clearTransitions()
        
        assertEquals("Should have no transitions after clear", 0, serviceManager.getRecentTransitions().size)
    }
    
    @Test
    fun testServiceManagerStateTracking() {
        // Initial state
        assertFalse("Service should not be running initially", serviceManager.isServiceRunning())
        
        // Update to running state
        serviceManager.updateServiceState(true)
        assertTrue("Service should be running after update", serviceManager.isServiceRunning())
        
        // Update to stopped state
        serviceManager.updateServiceState(false)
        assertFalse("Service should not be running after stop", serviceManager.isServiceRunning())
    }
    
    @Test
    fun testServiceStatistics() {
        // Add some transitions
        repeat(25) { index ->
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
        
        val stats = serviceManager.getServiceStatistics()
        
        assertEquals("Should have 25 transitions", 25, stats.totalTransitions)
        assertTrue("Service should be running", stats.isServiceRunning)
        assertTrue("Last heartbeat should be recent", stats.lastHeartbeat > 0)
        assertTrue("Current time should be set", stats.currentTime > 0)
        assertTrue("Current time should be >= last heartbeat", stats.currentTime >= stats.lastHeartbeat)
    }
    
    @Test
    fun testServiceManagerGetRecentTransitionsWithLimit() {
        // Add 20 transitions
        repeat(20) { index ->
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
        
        // Get limited results
        val recent5 = serviceManager.getRecentTransitions(5)
        val recent10 = serviceManager.getRecentTransitions(10)
        val recent30 = serviceManager.getRecentTransitions(30)
        
        assertEquals("Should return 5 transitions", 5, recent5.size)
        assertEquals("Should return 10 transitions", 10, recent10.size)
        assertEquals("Should return all 20 transitions (capped at limit)", 20, recent30.size)
    }
    
    @Test
    fun testAppContextCreation() {
        val context = AppContext(
            packageName = "com.example.testapp",
            activityName = "com.example.testapp.TestActivity",
            windowTitle = "Test Window Title",
            isScreenOn = true,
            timestamp = System.currentTimeMillis()
        )
        
        assertEquals("Package name should match", "com.example.testapp", context.packageName)
        assertEquals("Activity name should match", "com.example.testapp.TestActivity", context.activityName)
        assertEquals("Window title should match", "Test Window Title", context.windowTitle)
        assertTrue("Screen should be on", context.isScreenOn)
        assertTrue("Timestamp should be positive", context.timestamp > 0)
    }
    
    @Test
    fun testAppContextWithScreenOff() {
        val context = AppContext(
            packageName = "com.example.app",
            activityName = "MainActivity",
            windowTitle = "",
            isScreenOn = false,
            timestamp = System.currentTimeMillis()
        )
        
        assertFalse("Screen should be off", context.isScreenOn)
        assertTrue("Package name should not be empty", context.packageName.isNotEmpty())
    }
    
    @Test
    fun testAppTransitionEventTypes() {
        val windowStateTransition = AppTransition(
            timestamp = System.currentTimeMillis(),
            fromPackage = "com.example.old",
            toPackage = "com.example.new",
            fromActivity = "OldActivity",
            toActivity = "NewActivity",
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
        
        val contentChangedTransition = AppTransition(
            timestamp = System.currentTimeMillis(),
            fromPackage = "com.example.old",
            toPackage = "com.example.new",
            fromActivity = "OldActivity",
            toActivity = "NewActivity",
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        )
        
        assertEquals("Event type should be WINDOW_STATE_CHANGED", 
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, 
            windowStateTransition.eventType)
        assertEquals("Event type should be WINDOW_CONTENT_CHANGED", 
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, 
            contentChangedTransition.eventType)
        assertNotEquals("Event types should differ", 
            windowStateTransition.eventType, 
            contentChangedTransition.eventType)
    }
    
    @Test
    fun testUiTreeSnapshotWithEmptyTree() {
        val emptyRootNode = UiNode(
            nodeId = "root",
            className = "android.view.View",
            resourceName = "",
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
        
        val snapshot = UiTreeSnapshot(
            timestamp = System.currentTimeMillis(),
            packageName = "com.example.empty",
            activityName = "EmptyActivity",
            windowTitle = "Empty",
            rootNode = emptyRootNode
        )
        
        assertTrue("Root should have no children", snapshot.rootNode.children.isEmpty())
        assertEquals("Root resource name should be empty", "", snapshot.rootNode.resourceName)
        assertEquals("Root text should be empty", "", snapshot.rootNode.text)
    }
    
    @Test
    fun testUiTreeSnapshotWithComplexHierarchy() {
        // Create a complex 3-level hierarchy
        val leafNode1 = UiNode(
            nodeId = "leaf_1",
            className = "android.widget.TextView",
            resourceName = "id/text_1",
            contentDescription = "Text 1",
            text = "Hello",
            isClickable = false,
            isFocusable = false,
            isEnabled = true,
            isVisible = true,
            bounds = null,
            accessibilityAttributes = emptyMap(),
            children = emptyList()
        )
        
        val leafNode2 = UiNode(
            nodeId = "leaf_2",
            className = "android.widget.Button",
            resourceName = "id/button_1",
            contentDescription = "Button 1",
            text = "Click",
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            isVisible = true,
            bounds = null,
            accessibilityAttributes = emptyMap(),
            children = emptyList()
        )
        
        val intermediateNode = UiNode(
            nodeId = "intermediate",
            className = "android.widget.LinearLayout",
            resourceName = "id/layout_1",
            contentDescription = "",
            text = "",
            isClickable = false,
            isFocusable = false,
            isEnabled = true,
            isVisible = true,
            bounds = null,
            accessibilityAttributes = emptyMap(),
            children = listOf(leafNode1, leafNode2)
        )
        
        val rootNode = UiNode(
            nodeId = "root",
            className = "android.widget.FrameLayout",
            resourceName = "id/root",
            contentDescription = "",
            text = "",
            isClickable = false,
            isFocusable = false,
            isEnabled = true,
            isVisible = true,
            bounds = null,
            accessibilityAttributes = emptyMap(),
            children = listOf(intermediateNode)
        )
        
        val snapshot = UiTreeSnapshot(
            timestamp = System.currentTimeMillis(),
            packageName = "com.example.complex",
            activityName = "MainActivity",
            windowTitle = "Complex UI",
            rootNode = rootNode
        )
        
        assertEquals("Root should have 1 child", 1, snapshot.rootNode.children.size)
        assertEquals("Intermediate should have 2 children", 2, snapshot.rootNode.children[0].children.size)
        assertEquals("First leaf should be TextView", "android.widget.TextView", 
            snapshot.rootNode.children[0].children[0].className)
        assertEquals("Second leaf should be Button", "android.widget.Button", 
            snapshot.rootNode.children[0].children[1].className)
    }
    
    @Test
    fun testUiNodeAccessibilityAttributes() {
        val attributes = mapOf(
            "isEditable" to true,
            "isPassword" to false,
            "isScrollable" to true,
            "isSelected" to false,
            "roleDescription" to "Input Field",
            "hintText" to "Enter text here",
            "error" to "",
            "stateDescription" to "Empty"
        )
        
        val node = UiNode(
            nodeId = "input_node",
            className = "android.widget.EditText",
            resourceName = "id/input_field",
            contentDescription = "Input Field",
            text = "",
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            isVisible = true,
            bounds = null,
            accessibilityAttributes = attributes,
            children = emptyList()
        )
        
        assertEquals("Should have 8 attributes", 8, node.accessibilityAttributes.size)
        assertEquals("isEditable should be true", true, node.accessibilityAttributes["isEditable"])
        assertEquals("isPassword should be false", false, node.accessibilityAttributes["isPassword"])
        assertEquals("isScrollable should be true", true, node.accessibilityAttributes["isScrollable"])
        assertEquals("roleDescription should match", "Input Field", node.accessibilityAttributes["roleDescription"])
        assertEquals("hintText should match", "Enter text here", node.accessibilityAttributes["hintText"])
    }
    
    @Test
    fun testUiNodeVisibilityStates() {
        val visibleNode = UiNode(
            nodeId = "visible",
            className = "android.widget.Button",
            resourceName = "id/visible_button",
            contentDescription = "",
            text = "Visible",
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            isVisible = true,
            bounds = NodeBounds(0, 0, 100, 50, 100, 50),
            accessibilityAttributes = emptyMap(),
            children = emptyList()
        )
        
        val invisibleNode = UiNode(
            nodeId = "invisible",
            className = "android.widget.Button",
            resourceName = "id/invisible_button",
            contentDescription = "",
            text = "Invisible",
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            isVisible = false,
            bounds = null,
            accessibilityAttributes = emptyMap(),
            children = emptyList()
        )
        
        assertTrue("Visible node should be visible", visibleNode.isVisible)
        assertFalse("Invisible node should not be visible", invisibleNode.isVisible)
        assertNotNull("Visible node should have bounds", visibleNode.bounds)
        assertNull("Invisible node may have null bounds", invisibleNode.bounds)
    }
    
    @Test
    fun testUiNodeEnabledStates() {
        val enabledNode = UiNode(
            nodeId = "enabled",
            className = "android.widget.Button",
            resourceName = "id/enabled_button",
            contentDescription = "",
            text = "Enabled",
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            isVisible = true,
            bounds = null,
            accessibilityAttributes = emptyMap(),
            children = emptyList()
        )
        
        val disabledNode = UiNode(
            nodeId = "disabled",
            className = "android.widget.Button",
            resourceName = "id/disabled_button",
            contentDescription = "",
            text = "Disabled",
            isClickable = false,
            isFocusable = false,
            isEnabled = false,
            isVisible = true,
            bounds = null,
            accessibilityAttributes = emptyMap(),
            children = emptyList()
        )
        
        assertTrue("Enabled node should be enabled", enabledNode.isEnabled)
        assertFalse("Disabled node should not be enabled", disabledNode.isEnabled)
        assertTrue("Enabled node should be clickable", enabledNode.isClickable)
        assertFalse("Disabled node should not be clickable", disabledNode.isClickable)
    }
    
    @Test
    fun testAccessibilityCommandEquality() {
        val command1 = AccessibilityCommand(
            type = CommandType.CLICK_NODE,
            parameters = mapOf("nodeId" to "button_1"),
            timeoutMs = 5000
        )
        
        val command2 = AccessibilityCommand(
            type = CommandType.CLICK_NODE,
            parameters = mapOf("nodeId" to "button_1"),
            timeoutMs = 5000
        )
        
        val command3 = AccessibilityCommand(
            type = CommandType.SCROLL_NODE,
            parameters = mapOf("nodeId" to "list_1"),
            timeoutMs = 5000
        )
        
        assertEquals("Commands with same values should be equal", command1, command2)
        assertNotEquals("Commands with different types should not be equal", command1, command3)
    }
    
    @Test
    fun testScrollCommandParameters() {
        val scrollForwardCommand = AccessibilityCommand(
            type = CommandType.SCROLL_NODE,
            parameters = mapOf(
                "nodeId" to "scrollable_list",
                "direction" to "forward"
            ),
            timeoutMs = 3000
        )
        
        val scrollBackwardCommand = AccessibilityCommand(
            type = CommandType.SCROLL_NODE,
            parameters = mapOf(
                "nodeId" to "scrollable_list",
                "direction" to "backward"
            ),
            timeoutMs = 3000
        )
        
        assertEquals("Node ID should match", "scrollable_list", scrollForwardCommand.parameters["nodeId"])
        assertEquals("Direction should be forward", "forward", scrollForwardCommand.parameters["direction"])
        assertEquals("Direction should be backward", "backward", scrollBackwardCommand.parameters["direction"])
    }
    
    @Test
    fun testFindNodeByTextCommand() {
        val command = AccessibilityCommand(
            type = CommandType.FIND_NODE_BY_TEXT,
            parameters = mapOf(
                "text" to "Login",
                "caseSensitive" to false,
                "exactMatch" to false
            )
        )
        
        assertEquals("Type should be FIND_NODE_BY_TEXT", CommandType.FIND_NODE_BY_TEXT, command.type)
        assertEquals("Text should match", "Login", command.parameters["text"])
        assertEquals("Case sensitive should be false", false, command.parameters["caseSensitive"])
        assertEquals("Exact match should be false", false, command.parameters["exactMatch"])
    }
    
    @Test
    fun testSetTextCommand() {
        val command = AccessibilityCommand(
            type = CommandType.SET_TEXT,
            parameters = mapOf(
                "nodeId" to "edit_text_123",
                "text" to "Hello World",
                "clearExisting" to true
            ),
            timeoutMs = 7000
        )
        
        assertEquals("Type should be SET_TEXT", CommandType.SET_TEXT, command.type)
        assertEquals("Node ID should match", "edit_text_123", command.parameters["nodeId"])
        assertEquals("Text should match", "Hello World", command.parameters["text"])
        assertEquals("Clear existing should be true", true, command.parameters["clearExisting"])
        assertEquals("Timeout should be 7000ms", 7000, command.timeoutMs)
    }
    
    @Test
    fun testMultipleEventListeners() {
        val events = mutableListOf<AccessibilityEvent>()
        
        val listener1 = object : AccessibilityEventListener {
            override fun onAccessibilityEvent(event: AccessibilityEvent) {
                events.add(event)
            }
        }
        
        val listener2 = object : AccessibilityEventListener {
            override fun onAccessibilityEvent(event: AccessibilityEvent) {
                events.add(event)
            }
        }
        
        val mockEvent = mock(AccessibilityEvent::class.java)
        `when`(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_CLICKED)
        
        listener1.onAccessibilityEvent(mockEvent)
        listener2.onAccessibilityEvent(mockEvent)
        
        assertEquals("Should have 2 events recorded", 2, events.size)
    }
    
    @Test
    fun testMultipleContextListeners() {
        val transitions = mutableListOf<AppTransition>()
        
        val listener1 = object : AppContextListener {
            override fun onAppTransition(transition: AppTransition) {
                transitions.add(transition)
            }
        }
        
        val listener2 = object : AppContextListener {
            override fun onAppTransition(transition: AppTransition) {
                transitions.add(transition)
            }
        }
        
        val transition = AppTransition(
            timestamp = System.currentTimeMillis(),
            fromPackage = "old",
            toPackage = "new",
            fromActivity = "OldActivity",
            toActivity = "NewActivity",
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
        
        listener1.onAppTransition(transition)
        listener2.onAppTransition(transition)
        
        assertEquals("Should have 2 transitions recorded", 2, transitions.size)
        assertEquals("First transition should match", "new", transitions[0].toPackage)
        assertEquals("Second transition should match", "new", transitions[1].toPackage)
    }
    
    @Test
    fun testMultipleTreeListeners() {
        val snapshots = mutableListOf<UiTreeSnapshot>()
        
        val listener1 = object : UiTreeListener {
            override fun onTreeChanged(snapshot: UiTreeSnapshot) {
                snapshots.add(snapshot)
            }
        }
        
        val listener2 = object : UiTreeListener {
            override fun onTreeChanged(snapshot: UiTreeSnapshot) {
                snapshots.add(snapshot)
            }
        }
        
        val snapshot = UiTreeSnapshot(
            timestamp = System.currentTimeMillis(),
            packageName = "com.example.app",
            activityName = "MainActivity",
            windowTitle = "Main",
            rootNode = UiNode(
                nodeId = "root",
                className = "FrameLayout",
                resourceName = "",
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
        
        listener1.onTreeChanged(snapshot)
        listener2.onTreeChanged(snapshot)
        
        assertEquals("Should have 2 snapshots recorded", 2, snapshots.size)
        assertEquals("First snapshot package should match", "com.example.app", snapshots[0].packageName)
        assertEquals("Second snapshot package should match", "com.example.app", snapshots[1].packageName)
    }
    
    @Test
    fun testCommandResultWithComplexData() {
        val complexData = mapOf(
            "nodes" to listOf(
                UiNode(
                    nodeId = "node1",
                    className = "TextView",
                    resourceName = "id/text1",
                    contentDescription = "",
                    text = "Text 1",
                    isClickable = false,
                    isFocusable = false,
                    isEnabled = true,
                    isVisible = true,
                    bounds = null,
                    accessibilityAttributes = emptyMap(),
                    children = emptyList()
                )
            ),
            "count" to 1,
            "timestamp" to System.currentTimeMillis()
        )
        
        val result = CommandResult(
            success = true,
            data = complexData,
            error = null,
            timestamp = System.currentTimeMillis()
        )
        
        assertTrue("Result should be successful", result.success)
        assertNotNull("Data should not be null", result.data)
        
        @Suppress("UNCHECKED_CAST")
        val data = result.data as Map<String, Any>
        assertTrue("Data should contain nodes key", data.containsKey("nodes"))
        assertTrue("Data should contain count key", data.containsKey("count"))
        assertEquals("Count should be 1", 1, data["count"])
    }
    
    @Test
    fun testTransitionTimestampOrdering() {
        val transition1 = AppTransition(
            timestamp = 1000L,
            fromPackage = "app1",
            toPackage = "app2",
            fromActivity = "Activity1",
            toActivity = "Activity2",
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
        
        val transition2 = AppTransition(
            timestamp = 2000L,
            fromPackage = "app2",
            toPackage = "app3",
            fromActivity = "Activity2",
            toActivity = "Activity3",
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
        
        val transition3 = AppTransition(
            timestamp = 3000L,
            fromPackage = "app3",
            toPackage = "app4",
            fromActivity = "Activity3",
            toActivity = "Activity4",
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
        
        assertTrue("Transition 2 should be after Transition 1", transition2.timestamp > transition1.timestamp)
        assertTrue("Transition 3 should be after Transition 2", transition3.timestamp > transition2.timestamp)
        assertTrue("Transition 3 should be after Transition 1", transition3.timestamp > transition1.timestamp)
    }
    
    @Test
    fun testEmptyParametersCommand() {
        val command = AccessibilityCommand(
            type = CommandType.GET_TREE_SNAPSHOT,
            parameters = emptyMap()
        )
        
        assertTrue("Parameters should be empty", command.parameters.isEmpty())
        assertEquals("Should have 0 parameters", 0, command.parameters.size)
        assertFalse("Should not contain any keys", command.parameters.containsKey("anyKey"))
    }
    
    @Test
    fun testNodeIdGeneration() {
        val node1 = UiNode(
            nodeId = "android.widget.Button_id/submit_button_12345",
            className = "android.widget.Button",
            resourceName = "id/submit_button",
            contentDescription = "Submit",
            text = "Submit",
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            isVisible = true,
            bounds = null,
            accessibilityAttributes = emptyMap(),
            children = emptyList()
        )
        
        val node2 = UiNode(
            nodeId = "android.widget.Button_id/cancel_button_67890",
            className = "android.widget.Button",
            resourceName = "id/cancel_button",
            contentDescription = "Cancel",
            text = "Cancel",
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            isVisible = true,
            bounds = null,
            accessibilityAttributes = emptyMap(),
            children = emptyList()
        )
        
        assertNotEquals("Node IDs should be unique", node1.nodeId, node2.nodeId)
        assertTrue("Node ID should contain class name", node1.nodeId.contains("Button"))
        assertTrue("Node ID should contain resource name", node1.nodeId.contains("submit_button"))
    }
}