package com.genos.accessibility.aggregator

import com.genos.accessibility.model.UiBounds
import com.genos.accessibility.model.UiElement
import com.genos.accessibility.model.UiTree
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenStateAggregatorTest {

    private lateinit var aggregator: ScreenStateAggregator

    @Before
    fun setup() {
        aggregator = ScreenStateAggregator()
    }

    @Test
    fun `test initial state is null`() = runTest {
        // When
        val currentTree = aggregator.currentUiTree.value
        val currentPackage = aggregator.currentAppPackage.value
        val transitionEvent = aggregator.appTransitionEvents.value
        
        // Then
        assertNull(currentTree)
        assertNull(currentPackage)
        assertNull(transitionEvent)
    }

    @Test
    fun `test onAccessibilityEvent updates current package`() = runTest {
        // Given
        val packageName = "com.test.app"
        
        // When
        aggregator.onAccessibilityEvent(packageName)
        
        // Then
        assertEquals(packageName, aggregator.currentAppPackage.value)
    }

    @Test
    fun `test onAccessibilityEvent creates transition event`() = runTest {
        // Given
        val firstPackage = "com.test.app1"
        val secondPackage = "com.test.app2"
        
        // When
        aggregator.onAccessibilityEvent(firstPackage)
        aggregator.onAccessibilityEvent(secondPackage)
        
        // Then
        val transitionEvent = aggregator.appTransitionEvents.value
        assertNotNull(transitionEvent)
        assertEquals(firstPackage, transitionEvent?.previousPackage)
        assertEquals(secondPackage, transitionEvent?.newPackage)
        assertTrue(transitionEvent!!.timestamp > 0)
    }

    @Test
    fun `test onAccessibilityEvent does not create transition for same package`() = runTest {
        // Given
        val packageName = "com.test.app"
        
        // When
        aggregator.onAccessibilityEvent(packageName)
        val firstTransition = aggregator.appTransitionEvents.value
        aggregator.onAccessibilityEvent(packageName) // Same package
        val secondTransition = aggregator.appTransitionEvents.value
        
        // Then
        assertEquals(firstTransition, secondTransition) // Should not change
    }

    @Test
    fun `test onUiTreeUpdate updates current tree`() = runTest {
        // Given
        val element = UiElement(id = "test", text = "Test Element")
        val tree = UiTree(element, "com.test.app", System.currentTimeMillis())
        
        // When
        aggregator.onUiTreeUpdate(tree)
        
        // Then
        assertEquals(tree, aggregator.currentUiTree.value)
    }

    @Test
    fun `test onUiTreeUpdate adds to history`() = runTest {
        // Given
        val tree1 = UiTree(UiElement(id = "1"), "com.test.app1", 1000L)
        val tree2 = UiTree(UiElement(id = "2"), "com.test.app2", 2000L)
        
        // When
        aggregator.onUiTreeUpdate(tree1)
        aggregator.onUiTreeUpdate(tree2)
        
        // Then
        val history = aggregator.uiTreeHistory.value
        assertEquals(2, history.size)
        assertEquals(tree1, history[0])
        assertEquals(tree2, history[1])
    }

    @Test
    fun `test history maintains maximum size`() = runTest {
        // Given - Create more trees than MAX_HISTORY_SIZE (10)
        repeat(15) { index ->
            val tree = UiTree(
                UiElement(id = "element_$index"),
                "com.test.app",
                index.toLong()
            )
            aggregator.onUiTreeUpdate(tree)
        }
        
        // Then
        val history = aggregator.uiTreeHistory.value
        assertEquals(10, history.size) // Should maintain max size of 10
        assertEquals("element_5", history[0].root.id) // Oldest should be element_5
        assertEquals("element_14", history[9].root.id) // Newest should be element_14
    }

    @Test
    fun `test getCurrentUiTreeSnapshot returns current tree`() = runTest {
        // Given
        val tree = UiTree(UiElement(id = "test"), "com.test.app")
        aggregator.onUiTreeUpdate(tree)
        
        // When
        val snapshot = aggregator.getCurrentUiTreeSnapshot()
        
        // Then
        assertEquals(tree, snapshot)
    }

    @Test
    fun `test getCurrentUiTreeSnapshot returns null when no tree`() = runTest {
        // When
        val snapshot = aggregator.getCurrentUiTreeSnapshot()
        
        // Then
        assertNull(snapshot)
    }

    @Test
    fun `test getCurrentPackage returns current package`() = runTest {
        // Given
        val packageName = "com.test.app"
        aggregator.onAccessibilityEvent(packageName)
        
        // When
        val current = aggregator.getCurrentPackage()
        
        // Then
        assertEquals(packageName, current)
    }

    @Test
    fun `test getSerializedUiTree returns valid JSON`() = runTest {
        // Given
        val element = UiElement(
            id = "test_id",
            text = "Test Text",
            className = "Button"
        )
        val tree = UiTree(element, "com.test.app")
        aggregator.onUiTreeUpdate(tree)
        
        // When
        val serialized = aggregator.getSerializedUiTree()
        
        // Then
        assertNotNull(serialized)
        assertTrue(serialized!!.contains("test_id"))
        assertTrue(serialized.contains("Test Text"))
        assertTrue(serialized.contains("Button"))
        assertTrue(serialized.contains("com.test.app"))
    }

    @Test
    fun `test getSerializedUiTree returns null when no tree`() = runTest {
        // When
        val serialized = aggregator.getSerializedUiTree()
        
        // Then
        assertNull(serialized)
    }

    @Test
    fun `test getSerializedElement returns valid JSON`() = runTest {
        // Given
        val element = UiElement(
            id = "test_id",
            text = "Test Text",
            isClickable = true
        )
        
        // When
        val serialized = aggregator.getSerializedElement(element)
        
        // Then
        assertNotNull(serialized)
        assertTrue(serialized!!.contains("test_id"))
        assertTrue(serialized.contains("Test Text"))
        assertTrue(serialized.contains("true"))
    }

    @Test
    fun `test clear resets all state`() = runTest {
        // Given
        val tree = UiTree(UiElement(id = "test"), "com.test.app")
        aggregator.onAccessibilityEvent("com.test.app")
        aggregator.onUiTreeUpdate(tree)
        
        // When
        aggregator.clear()
        
        // Then
        assertNull(aggregator.currentUiTree.value)
        assertNull(aggregator.currentAppPackage.value)
        assertNull(aggregator.appTransitionEvents.value)
        assertTrue(aggregator.uiTreeHistory.value.isEmpty())
    }

    @Test
    fun `test getUiTreeHistory returns correct history`() = runTest {
        // Given
        val tree1 = UiTree(UiElement(id = "1"), "com.test.app1")
        val tree2 = UiTree(UiElement(id = "2"), "com.test.app2")
        aggregator.onUiTreeUpdate(tree1)
        aggregator.onUiTreeUpdate(tree2)
        
        // When
        val history = aggregator.getUiTreeHistory()
        
        // Then
        assertEquals(2, history.size)
        assertEquals(tree1, history[0])
        assertEquals(tree2, history[1])
    }

    @Test
    fun `test multiple package transitions`() = runTest {
        // Given
        val packages = listOf("com.app1", "com.app2", "com.app3")
        
        // When
        packages.forEach { packageName ->
            aggregator.onAccessibilityEvent(packageName)
        }
        
        // Then
        assertEquals("com.app3", aggregator.currentAppPackage.value)
        val lastTransition = aggregator.appTransitionEvents.value
        assertEquals("com.app2", lastTransition?.previousPackage)
        assertEquals("com.app3", lastTransition?.newPackage)
    }

    @Test
    fun `test onUiTreeUpdate with complex tree structure`() = runTest {
        // Given
        val child1 = UiElement(id = "child1", text = "Child 1")
        val child2 = UiElement(id = "child2", text = "Child 2")
        val parent = UiElement(
            id = "parent",
            text = "Parent",
            children = listOf(child1, child2)
        )
        val tree = UiTree(parent, "com.test.app")
        
        // When
        aggregator.onUiTreeUpdate(tree)
        
        // Then
        val currentTree = aggregator.currentUiTree.value
        assertNotNull(currentTree)
        assertEquals(2, currentTree!!.root.children.size)
        assertEquals("child1", currentTree.root.children[0].id)
        assertEquals("child2", currentTree.root.children[1].id)
    }

    @Test
    fun `test serialization with nested elements`() = runTest {
        // Given
        val child = UiElement(id = "child", text = "Child", isClickable = true)
        val parent = UiElement(
            id = "parent",
            text = "Parent",
            children = listOf(child)
        )
        
        // When
        val serialized = aggregator.getSerializedElement(parent)
        
        // Then
        assertNotNull(serialized)
        assertTrue(serialized!!.contains("parent"))
        assertTrue(serialized.contains("child"))
    }

    @Test
    fun `test AppTransitionEvent data class`() {
        // When
        val event = AppTransitionEvent(
            previousPackage = "com.app1",
            newPackage = "com.app2",
            timestamp = 12345L
        )
        
        // Then
        assertEquals("com.app1", event.previousPackage)
        assertEquals("com.app2", event.newPackage)
        assertEquals(12345L, event.timestamp)
    }

    @Test
    fun `test AppTransitionEvent with null previous package`() {
        // When
        val event = AppTransitionEvent(
            previousPackage = null,
            newPackage = "com.app2",
            timestamp = 12345L
        )
        
        // Then
        assertNull(event.previousPackage)
        assertEquals("com.app2", event.newPackage)
    }

    @Test
    fun `test concurrent updates`() = runTest {
        // Given
        val tree1 = UiTree(UiElement(id = "1"), "com.test.app1")
        val tree2 = UiTree(UiElement(id = "2"), "com.test.app2")
        
        // When
        aggregator.onUiTreeUpdate(tree1)
        aggregator.onAccessibilityEvent("com.test.app1")
        aggregator.onUiTreeUpdate(tree2)
        aggregator.onAccessibilityEvent("com.test.app2")
        
        // Then
        assertEquals(tree2, aggregator.currentUiTree.value)
        assertEquals("com.test.app2", aggregator.currentAppPackage.value)
        assertEquals(2, aggregator.uiTreeHistory.value.size)
    }

    @Test
    fun `test element counting with nested structure`() = runTest {
        // Given
        val grandchild = UiElement(id = "grandchild")
        val child1 = UiElement(id = "child1", children = listOf(grandchild))
        val child2 = UiElement(id = "child2")
        val root = UiElement(id = "root", children = listOf(child1, child2))
        val tree = UiTree(root, "com.test.app")
        
        // When
        aggregator.onUiTreeUpdate(tree)
        val serialized = aggregator.getSerializedUiTree()
        
        // Then
        assertNotNull(serialized)
        assertTrue(serialized!!.contains("root"))
        assertTrue(serialized.contains("child1"))
        assertTrue(serialized.contains("child2"))
        assertTrue(serialized.contains("grandchild"))
    }
}