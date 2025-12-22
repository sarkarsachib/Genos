package com.genos.accessibility.model

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UiElementTest {

    @Test
    fun `test UiElement creation with all properties`() {
        // Given
        val bounds = UiBounds(0, 10, 100, 200)
        val boundsInScreen = UiBounds(0, 20, 100, 210)
        val timestamp = System.currentTimeMillis()
        
        // When
        val element = UiElement(
            id = "test_id",
            className = "android.widget.Button",
            packageName = "com.test.app",
            text = "Click Me",
            contentDescription = "Test Button",
            viewIdResourceName = "btn_test",
            bounds = bounds,
            boundsInScreen = boundsInScreen,
            isClickable = true,
            isEnabled = true,
            isFocusable = true,
            isFocused = false,
            isScrollable = false,
            isSelected = false,
            isVisible = true,
            children = emptyList(),
            timestamp = timestamp
        )
        
        // Then
        assertEquals("test_id", element.id)
        assertEquals("android.widget.Button", element.className)
        assertEquals("com.test.app", element.packageName)
        assertEquals("Click Me", element.text)
        assertEquals("Test Button", element.contentDescription)
        assertEquals("btn_test", element.viewIdResourceName)
        assertEquals(bounds, element.bounds)
        assertEquals(boundsInScreen, element.boundsInScreen)
        assertTrue(element.isClickable)
        assertTrue(element.isEnabled)
        assertTrue(element.isFocusable)
        assertFalse(element.isFocused)
        assertFalse(element.isScrollable)
        assertFalse(element.isSelected)
        assertTrue(element.isVisible)
        assertTrue(element.children.isEmpty())
        assertEquals(timestamp, element.timestamp)
    }

    @Test
    fun `test UiElement with default values`() {
        // When
        val element = UiElement()
        
        // Then
        assertNull(element.id)
        assertNull(element.className)
        assertNull(element.packageName)
        assertNull(element.text)
        assertNull(element.contentDescription)
        assertNull(element.viewIdResourceName)
        assertNull(element.bounds)
        assertNull(element.boundsInScreen)
        assertFalse(element.isClickable)
        assertTrue(element.isEnabled)
        assertFalse(element.isFocusable)
        assertFalse(element.isFocused)
        assertFalse(element.isScrollable)
        assertFalse(element.isSelected)
        assertTrue(element.isVisible)
        assertTrue(element.children.isEmpty())
        assertTrue(element.timestamp > 0)
    }

    @Test
    fun `test UiElement with children`() {
        // Given
        val child1 = UiElement(id = "child1", text = "Child 1")
        val child2 = UiElement(id = "child2", text = "Child 2")
        
        // When
        val parent = UiElement(
            id = "parent",
            text = "Parent",
            children = listOf(child1, child2)
        )
        
        // Then
        assertEquals(2, parent.children.size)
        assertEquals("child1", parent.children[0].id)
        assertEquals("child2", parent.children[1].id)
    }

    @Test
    fun `test UiBounds creation from coordinates`() {
        // When
        val bounds = UiBounds(10, 20, 100, 200)
        
        // Then
        assertEquals(10, bounds.left)
        assertEquals(20, bounds.top)
        assertEquals(100, bounds.right)
        assertEquals(200, bounds.bottom)
    }

    @Test
    fun `test UiBounds creation from Rect`() {
        // Given
        val rect = Rect(10, 20, 100, 200)
        
        // When
        val bounds = UiBounds(rect)
        
        // Then
        assertEquals(10, bounds.left)
        assertEquals(20, bounds.top)
        assertEquals(100, bounds.right)
        assertEquals(200, bounds.bottom)
    }

    @Test
    fun `test UiBounds with zero dimensions`() {
        // When
        val bounds = UiBounds(0, 0, 0, 0)
        
        // Then
        assertEquals(0, bounds.left)
        assertEquals(0, bounds.top)
        assertEquals(0, bounds.right)
        assertEquals(0, bounds.bottom)
    }

    @Test
    fun `test UiBounds with negative coordinates`() {
        // When
        val bounds = UiBounds(-10, -20, 100, 200)
        
        // Then
        assertEquals(-10, bounds.left)
        assertEquals(-20, bounds.top)
        assertEquals(100, bounds.right)
        assertEquals(200, bounds.bottom)
    }

    @Test
    fun `test UiTree creation`() {
        // Given
        val root = UiElement(id = "root", text = "Root")
        val packageName = "com.test.app"
        val timestamp = System.currentTimeMillis()
        
        // When
        val tree = UiTree(root, packageName, timestamp)
        
        // Then
        assertEquals(root, tree.root)
        assertEquals(packageName, tree.packageName)
        assertEquals(timestamp, tree.timestamp)
    }

    @Test
    fun `test UiTree with nested elements`() {
        // Given
        val child = UiElement(id = "child", text = "Child")
        val root = UiElement(id = "root", text = "Root", children = listOf(child))
        
        // When
        val tree = UiTree(root, "com.test.app")
        
        // Then
        assertEquals(1, tree.root.children.size)
        assertEquals("child", tree.root.children[0].id)
    }

    @Test
    fun `test AccessibilityNodeInfo toUiElement conversion`() {
        // Given
        val nodeInfo = mockk<AccessibilityNodeInfo>(relaxed = true)
        val parentRect = Rect(10, 20, 100, 200)
        val screenRect = Rect(10, 30, 100, 210)
        
        every { nodeInfo.viewIdResourceName } returns "test_view_id"
        every { nodeInfo.className } returns "android.widget.TextView"
        every { nodeInfo.packageName } returns "com.test.app"
        every { nodeInfo.text } returns "Test Text"
        every { nodeInfo.contentDescription } returns "Test Description"
        every { nodeInfo.isClickable } returns true
        every { nodeInfo.isEnabled } returns true
        every { nodeInfo.isFocusable } returns true
        every { nodeInfo.isFocused } returns false
        every { nodeInfo.isScrollable } returns false
        every { nodeInfo.isSelected } returns false
        every { nodeInfo.isVisibleToUser } returns true
        every { nodeInfo.childCount } returns 0
        every { nodeInfo.getBoundsInParent(any()) } answers {
            firstArg<Rect>().set(parentRect)
        }
        every { nodeInfo.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(screenRect)
        }
        
        // When
        val element = nodeInfo.toUiElement()
        
        // Then
        assertEquals("test_view_id", element.id)
        assertEquals("android.widget.TextView", element.className)
        assertEquals("com.test.app", element.packageName)
        assertEquals("Test Text", element.text)
        assertEquals("Test Description", element.contentDescription)
        assertTrue(element.isClickable)
        assertTrue(element.isEnabled)
        assertTrue(element.isFocusable)
        assertFalse(element.isFocused)
        assertFalse(element.isScrollable)
        assertFalse(element.isSelected)
        assertTrue(element.isVisible)
        assertEquals(10, element.bounds?.left)
        assertEquals(20, element.bounds?.top)
        assertEquals(100, element.bounds?.right)
        assertEquals(200, element.bounds?.bottom)
        
        verify { nodeInfo.getBoundsInParent(any()) }
        verify { nodeInfo.getBoundsInScreen(any()) }
    }

    @Test
    fun `test AccessibilityNodeInfo toUiElement with null values`() {
        // Given
        val nodeInfo = mockk<AccessibilityNodeInfo>(relaxed = true)
        
        every { nodeInfo.viewIdResourceName } returns null
        every { nodeInfo.className } returns null
        every { nodeInfo.packageName } returns null
        every { nodeInfo.text } returns null
        every { nodeInfo.contentDescription } returns null
        every { nodeInfo.isClickable } returns false
        every { nodeInfo.isEnabled } returns false
        every { nodeInfo.isFocusable } returns false
        every { nodeInfo.isFocused } returns false
        every { nodeInfo.isScrollable } returns false
        every { nodeInfo.isSelected } returns false
        every { nodeInfo.isVisibleToUser } returns false
        every { nodeInfo.childCount } returns 0
        every { nodeInfo.getBoundsInParent(any()) } answers {
            firstArg<Rect>().set(0, 0, 0, 0)
        }
        every { nodeInfo.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(0, 0, 0, 0)
        }
        
        // When
        val element = nodeInfo.toUiElement()
        
        // Then
        assertNull(element.id)
        assertNull(element.className)
        assertNull(element.text)
        assertNull(element.contentDescription)
        assertFalse(element.isClickable)
        assertFalse(element.isEnabled)
        assertFalse(element.isVisible)
    }

    @Test
    fun `test AccessibilityNodeInfo toUiElement with children`() {
        // Given
        val childNode1 = mockk<AccessibilityNodeInfo>(relaxed = true)
        val childNode2 = mockk<AccessibilityNodeInfo>(relaxed = true)
        val parentNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        
        every { childNode1.viewIdResourceName } returns "child1"
        every { childNode1.text } returns "Child 1"
        every { childNode1.childCount } returns 0
        every { childNode1.getBoundsInParent(any()) } answers {
            firstArg<Rect>().set(0, 0, 50, 50)
        }
        every { childNode1.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(0, 0, 50, 50)
        }
        
        every { childNode2.viewIdResourceName } returns "child2"
        every { childNode2.text } returns "Child 2"
        every { childNode2.childCount } returns 0
        every { childNode2.getBoundsInParent(any()) } answers {
            firstArg<Rect>().set(0, 50, 50, 100)
        }
        every { childNode2.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(0, 50, 50, 100)
        }
        
        every { parentNode.viewIdResourceName } returns "parent"
        every { parentNode.text } returns "Parent"
        every { parentNode.childCount } returns 2
        every { parentNode.getChild(0) } returns childNode1
        every { parentNode.getChild(1) } returns childNode2
        every { parentNode.getBoundsInParent(any()) } answers {
            firstArg<Rect>().set(0, 0, 100, 100)
        }
        every { parentNode.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(0, 0, 100, 100)
        }
        
        // When
        val element = parentNode.toUiElement()
        
        // Then
        assertEquals("parent", element.id)
        assertEquals(2, element.children.size)
        assertEquals("child1", element.children[0].id)
        assertEquals("Child 1", element.children[0].text)
        assertEquals("child2", element.children[1].id)
        assertEquals("Child 2", element.children[1].text)
    }

    @Test
    fun `test AccessibilityNodeInfo toUiElement with null child`() {
        // Given
        val parentNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        
        every { parentNode.viewIdResourceName } returns "parent"
        every { parentNode.childCount } returns 2
        every { parentNode.getChild(0) } returns null
        every { parentNode.getChild(1) } returns mockk<AccessibilityNodeInfo>(relaxed = true) {
            every { viewIdResourceName } returns "child2"
            every { childCount } returns 0
            every { getBoundsInParent(any()) } answers {
                firstArg<Rect>().set(0, 0, 50, 50)
            }
            every { getBoundsInScreen(any()) } answers {
                firstArg<Rect>().set(0, 0, 50, 50)
            }
        }
        every { parentNode.getBoundsInParent(any()) } answers {
            firstArg<Rect>().set(0, 0, 100, 100)
        }
        every { parentNode.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(0, 0, 100, 100)
        }
        
        // When
        val element = parentNode.toUiElement()
        
        // Then
        assertEquals(1, element.children.size) // Only non-null child
        assertEquals("child2", element.children[0].id)
    }

    @Test
    fun `test UiElement data class copy`() {
        // Given
        val original = UiElement(
            id = "original",
            text = "Original Text",
            isClickable = true
        )
        
        // When
        val copy = original.copy(text = "Modified Text")
        
        // Then
        assertEquals("original", copy.id)
        assertEquals("Modified Text", copy.text)
        assertTrue(copy.isClickable)
    }

    @Test
    fun `test UiElement equality`() {
        // Given
        val element1 = UiElement(id = "test", text = "Test")
        val element2 = UiElement(id = "test", text = "Test")
        
        // Then
        assertEquals(element1, element2)
        assertEquals(element1.hashCode(), element2.hashCode())
    }

    @Test
    fun `test UiBounds equality`() {
        // Given
        val bounds1 = UiBounds(10, 20, 100, 200)
        val bounds2 = UiBounds(10, 20, 100, 200)
        
        // Then
        assertEquals(bounds1, bounds2)
        assertEquals(bounds1.hashCode(), bounds2.hashCode())
    }

    @Test
    fun `test UiTree equality`() {
        // Given
        val root = UiElement(id = "root")
        val tree1 = UiTree(root, "com.test", 12345L)
        val tree2 = UiTree(root, "com.test", 12345L)
        
        // Then
        assertEquals(tree1, tree2)
        assertEquals(tree1.hashCode(), tree2.hashCode())
    }
}