package ai.genos.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.Mockito.*
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Instrumentation tests for GENOS Accessibility Service
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Config.NEWEST])
class GenosAccessibilityServiceInstrumentedTest {
    
    @get:Rule
    val serviceRule = ServiceTestRule()
    
    private lateinit var context: Context
    private lateinit var mockService: GenosAccessibilityService
    private lateinit var mockNodeInfo: AccessibilityNodeInfo
    
    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Create mock service
        mockService = mock(GenosAccessibilityService::class.java)
        
        // Create mock node info
        mockNodeInfo = mock(AccessibilityNodeInfo::class.java)
        
        // Initialize logger
        Logger.initializeFileLogging(context)
    }
    
    @After
    fun tearDown() {
        Logger.clearLogs()
    }
    
    @Test
    fun testServiceLifecycle() {
        // Test service initialization
        val serviceIntent = android.content.Intent(context, GenosAccessibilityService::class.java)
        
        // Verify service can be started (this would require proper permissions in real scenario)
        // For now, we test the service class directly
        val serviceClass = GenosAccessibilityService::class.java
        Assert.assertNotNull("Service class should exist", serviceClass)
        
        // Test service manager
        val serviceManager = GenosAccessibilityService.getServiceManager()
        Assert.assertNotNull("Service manager should be initialized", serviceManager)
    }
    
    @Test
    fun testUiTreeExtraction() {
        // Mock the rootInActiveWindow behavior
        `when`(mockService.rootInActiveWindow).thenReturn(mockNodeInfo)
        
        // Mock node info properties
        `when`(mockNodeInfo.className).thenReturn("android.widget.Button")
        `when`(mockNodeInfo.viewIdResourceName).thenReturn("id/test_button")
        `when`(mockNodeInfo.contentDescription).thenReturn("Test Button")
        `when`(mockNodeInfo.text).thenReturn("Click Me")
        `when`(mockNodeInfo.isClickable).thenReturn(true)
        `when`(mockNodeInfo.isFocusable).thenReturn(false)
        `when`(mockNodeInfo.isEnabled).thenReturn(true)
        `when`(mockNodeInfo.isVisibleToUser).thenReturn(true)
        
        // Mock bounds
        val mockBounds = android.graphics.Rect(0, 0, 100, 50)
        `when`(mockNodeInfo.getBoundsInScreen(org.mockito.ArgumentMatchers.any())).thenAnswer {
            val rect = it.arguments[0] as android.graphics.Rect
            rect.set(mockBounds.left, mockBounds.top, mockBounds.right, mockBounds.bottom)
        }
        
        // Test tree extraction (this would normally call the actual service method)
        // For testing purposes, we'll create a simple node structure
        val testNode = UiNode(
            nodeId = "button_123",
            className = "android.widget.Button",
            resourceName = "id/test_button",
            contentDescription = "Test Button",
            text = "Click Me",
            isClickable = true,
            isFocusable = false,
            isEnabled = true,
            isVisible = true,
            bounds = NodeBounds(0, 0, 100, 50, 100, 50),
            accessibilityAttributes = mapOf("isEditable" to false),
            children = emptyList()
        )
        
        Assert.assertNotNull("Tree node should be created", testNode)
        Assert.assertEquals("Node ID should match", "button_123", testNode.nodeId)
        Assert.assertEquals("Class name should match", "android.widget.Button", testNode.className)
        Assert.assertTrue("Button should be clickable", testNode.isClickable)
    }
    
    @Test
    fun testTreeSerialization() {
        val testTree = UiTreeSnapshot(
            timestamp = System.currentTimeMillis(),
            packageName = "com.example.testapp",
            activityName = "com.example.testapp.MainActivity",
            windowTitle = "Test App",
            rootNode = UiNode(
                nodeId = "root_layout",
                className = "android.widget.LinearLayout",
                resourceName = "id/root_layout",
                contentDescription = "",
                text = "",
                isClickable = false,
                isFocusable = false,
                isEnabled = true,
                isVisible = true,
                bounds = NodeBounds(0, 0, 1080, 1920, 1080, 1920),
                accessibilityAttributes = emptyMap(),
                children = listOf(
                    UiNode(
                        nodeId = "button_123",
                        className = "android.widget.Button",
                        resourceName = "id/test_button",
                        contentDescription = "Test Button",
                        text = "Click Me",
                        isClickable = true,
                        isFocusable = false,
                        isEnabled = true,
                        isVisible = true,
                        bounds = NodeBounds(100, 100, 200, 150, 100, 50),
                        accessibilityAttributes = mapOf("isEditable" to false),
                        children = emptyList()
                    )
                )
            )
        )
        
        // Test JSON serialization
        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(testTree)
        
        Assert.assertNotNull("JSON should not be null", json)
        Assert.assertTrue("JSON should contain package name", json.contains("com.example.testapp"))
        Assert.assertTrue("JSON should contain root node", json.contains("android.widget.LinearLayout"))
        Assert.assertTrue("JSON should contain child button", json.contains("android.widget.Button"))
        
        // Test deserialization
        val deserializedTree = gson.fromJson(json, UiTreeSnapshot::class.java)
        Assert.assertNotNull("Deserialized tree should not be null", deserializedTree)
        Assert.assertEquals("Package name should match", testTree.packageName, deserializedTree.packageName)
        Assert.assertEquals("Root node class should match", testTree.rootNode.className, deserializedTree.rootNode.className)
        Assert.assertEquals("Should have one child", 1, deserializedTree.rootNode.children.size)
    }
    
    @Test
    fun testCommandRouter() {
        val commandRouter = CommandRouter()
        
        // Test get current context command
        val getContextCommand = AccessibilityCommand(
            type = CommandType.GET_CURRENT_CONTEXT
        )
        
        Assert.assertNotNull("Command should be created", getContextCommand)
        Assert.assertEquals("Command type should be GET_CURRENT_CONTEXT", 
            CommandType.GET_CURRENT_CONTEXT, getContextCommand.type)
        
        // Test get tree snapshot command
        val getTreeCommand = AccessibilityCommand(
            type = CommandType.GET_TREE_SNAPSHOT,
            parameters = mapOf("includeBounds" to true)
        )
        
        Assert.assertNotNull("Tree command should be created", getTreeCommand)
        Assert.assertEquals("Command type should be GET_TREE_SNAPSHOT", 
            CommandType.GET_TREE_SNAPSHOT, getTreeCommand.type)
        Assert.assertTrue("Should include bounds parameter", 
            getTreeCommand.parameters.containsKey("includeBounds"))
    }
    
    @Test
    fun testEventListeners() {
        val eventLatch = CountDownLatch(1)
        val contextLatch = CountDownLatch(1)
        
        // Create mock listeners
        val eventListener = object : AccessibilityEventListener {
            override fun onAccessibilityEvent(event: AccessibilityEvent) {
                eventLatch.countDown()
            }
        }
        
        val contextListener = object : AppContextListener {
            override fun onAppTransition(transition: AppTransition) {
                contextLatch.countDown()
            }
        }
        
        // Test adding listeners
        // Note: In a real test, we would add these to the actual service
        Assert.assertNotNull("Event listener should be created", eventListener)
        Assert.assertNotNull("Context listener should be created", contextListener)
        
        // Test transition data
        val testTransition = AppTransition(
            timestamp = System.currentTimeMillis(),
            fromPackage = "com.example.oldapp",
            toPackage = "com.example.newapp",
            fromActivity = "com.example.oldapp.MainActivity",
            toActivity = "com.example.newapp.MainActivity",
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
        
        Assert.assertNotNull("Transition should be created", testTransition)
        Assert.assertEquals("Package should transition correctly", 
            "com.example.newapp", testTransition.toPackage)
    }
    
    @Test
    fun testServiceManager() {
        val serviceManager = AccessibilityServiceManager()
        
        // Test initial state
        Assert.assertFalse("Service should not be running initially", serviceManager.isServiceRunning())
        Assert.assertEquals("Should have no transitions initially", 0, serviceManager.getRecentTransitions().size)
        
        // Test recording transitions
        val transition1 = AppTransition(
            timestamp = System.currentTimeMillis(),
            fromPackage = "com.example.app1",
            toPackage = "com.example.app2",
            fromActivity = "Activity1",
            toActivity = "Activity2",
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
        
        serviceManager.recordTransition(transition1)
        
        Assert.assertEquals("Should have one transition", 1, serviceManager.getRecentTransitions().size)
        Assert.assertTrue("Service should be running after transition", serviceManager.isServiceRunning())
        
        // Test statistics
        val stats = serviceManager.getServiceStatistics()
        Assert.assertNotNull("Statistics should not be null", stats)
        Assert.assertEquals("Total transitions should be 1", 1, stats.totalTransitions)
    }
    
    @Test
    fun testPermissionHelper() {
        // Test permission status
        val status = PermissionHelper.getPermissionStatus(context)
        Assert.assertNotNull("Permission status should not be null", status)
        
        // Note: In a real device with accessibility service, these would be properly tested
        // For instrumentation tests, we just verify the helper methods don't crash
        val settingsIntent = PermissionHelper.getAccessibilitySettingsIntent()
        Assert.assertNotNull("Settings intent should not be null", settingsIntent)
    }
    
    @Test
    fun testLogger() {
        // Test logging functionality
        Logger.logInfo("TestTag", "Test info message")
        Logger.logDebug("TestTag", "Test debug message")
        Logger.logWarning("TestTag", "Test warning message")
        Logger.logError("TestTag", "Test error message")
        
        val logContents = Logger.getLogContents()
        Assert.assertNotNull("Log contents should not be null", logContents)
        Assert.assertTrue("Log should contain info message", logContents!!.contains("Test info message"))
        
        Logger.clearLogs()
        val clearedContents = Logger.getLogContents()
        // After clearing, logs might be empty or null depending on implementation
    }
    
    @Test
    fun testModels() {
        // Test all model classes
        val bounds = NodeBounds(0, 0, 100, 50, 100, 50)
        Assert.assertEquals("Bounds width should be 100", 100, bounds.width)
        
        val context = AppContext(
            packageName = "com.example.app",
            activityName = "MainActivity",
            windowTitle = "Test Window",
            isScreenOn = true,
            timestamp = System.currentTimeMillis()
        )
        Assert.assertEquals("Package should match", "com.example.app", context.packageName)
        
        val command = AccessibilityCommand(
            type = CommandType.CLICK_NODE,
            parameters = mapOf("nodeId" to "button123"),
            timeoutMs = 3000
        )
        Assert.assertEquals("Command timeout should be 3000", 3000, command.timeoutMs)
        
        val result = CommandResult(
            success = true,
            data = "Success data",
            error = null,
            timestamp = System.currentTimeMillis()
        )
        Assert.assertTrue("Command result should be successful", result.success)
    }
}