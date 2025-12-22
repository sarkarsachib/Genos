package com.genos.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.genos.accessibility.model.UiBounds
import com.genos.accessibility.model.UiElement
import com.genos.accessibility.model.UiTree
import io.mockk.every
import io.mockk.mockk

/**
 * Test utilities and helper functions for creating mock objects and test data.
 */
object TestUtils {

    /**
     * Creates a mock AccessibilityNodeInfo with default values.
     */
    fun createMockNodeInfo(
        viewId: String? = "test_view_id",
        className: String? = "android.widget.View",
        packageName: String? = "com.test.app",
        text: String? = "Test Text",
        contentDesc: String? = null,
        isClickable: Boolean = false,
        isEnabled: Boolean = true,
        isFocusable: Boolean = false,
        isScrollable: Boolean = false,
        isVisible: Boolean = true,
        childCount: Int = 0,
        children: List<AccessibilityNodeInfo> = emptyList()
    ): AccessibilityNodeInfo {
        val nodeInfo = mockk<AccessibilityNodeInfo>(relaxed = true)
        
        every { nodeInfo.viewIdResourceName } returns viewId
        every { nodeInfo.className } returns className
        every { nodeInfo.packageName } returns packageName
        every { nodeInfo.text } returns text
        every { nodeInfo.contentDescription } returns contentDesc
        every { nodeInfo.isClickable } returns isClickable
        every { nodeInfo.isEnabled } returns isEnabled
        every { nodeInfo.isFocusable } returns isFocusable
        every { nodeInfo.isFocused } returns false
        every { nodeInfo.isScrollable } returns isScrollable
        every { nodeInfo.isSelected } returns false
        every { nodeInfo.isVisibleToUser } returns isVisible
        every { nodeInfo.childCount } returns childCount
        
        children.forEachIndexed { index, child ->
            every { nodeInfo.getChild(index) } returns child
        }
        
        every { nodeInfo.getBoundsInParent(any()) } answers {
            firstArg<Rect>().set(0, 0, 100, 100)
        }
        every { nodeInfo.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(0, 0, 100, 100)
        }
        
        return nodeInfo
    }

    /**
     * Creates a UiElement with default test values.
     */
    fun createTestUiElement(
        id: String = "test_id",
        text: String? = "Test Text",
        className: String = "android.widget.View",
        isClickable: Boolean = false,
        children: List<UiElement> = emptyList()
    ): UiElement {
        return UiElement(
            id = id,
            className = className,
            packageName = "com.test.app",
            text = text,
            contentDescription = null,
            viewIdResourceName = id,
            bounds = UiBounds(0, 0, 100, 100),
            boundsInScreen = UiBounds(0, 0, 100, 100),
            isClickable = isClickable,
            isEnabled = true,
            isFocusable = false,
            isFocused = false,
            isScrollable = false,
            isSelected = false,
            isVisible = true,
            children = children,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Creates a UiTree with a simple root element.
     */
    fun createTestUiTree(
        rootId: String = "root",
        packageName: String = "com.test.app",
        childCount: Int = 0
    ): UiTree {
        val children = (0 until childCount).map { index ->
            createTestUiElement(id = "child_$index", text = "Child $index")
        }
        
        val root = createTestUiElement(id = rootId, text = "Root", children = children)
        
        return UiTree(
            root = root,
            packageName = packageName,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Creates a complex nested UI tree for testing.
     */
    fun createComplexUiTree(): UiTree {
        val grandchild1 = createTestUiElement(id = "grandchild1", text = "Grandchild 1")
        val grandchild2 = createTestUiElement(id = "grandchild2", text = "Grandchild 2")
        
        val child1 = createTestUiElement(
            id = "child1",
            text = "Child 1",
            children = listOf(grandchild1, grandchild2)
        )
        
        val child2 = createTestUiElement(id = "child2", text = "Child 2", isClickable = true)
        val child3 = createTestUiElement(id = "child3", text = "Child 3")
        
        val root = createTestUiElement(
            id = "root",
            text = "Root Container",
            children = listOf(child1, child2, child3)
        )
        
        return UiTree(
            root = root,
            packageName = "com.test.complex",
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Counts total number of elements in a UI tree.
     */
    fun countElements(element: UiElement): Int {
        return 1 + element.children.sumOf { countElements(it) }
    }

    /**
     * Finds an element by ID in a UI tree.
     */
    fun findElementById(element: UiElement, targetId: String): UiElement? {
        if (element.id == targetId) return element
        
        for (child in element.children) {
            val found = findElementById(child, targetId)
            if (found != null) return found
        }
        
        return null
    }

    /**
     * Collects all clickable elements from a UI tree.
     */
    fun collectClickableElements(element: UiElement): List<UiElement> {
        val clickable = mutableListOf<UiElement>()
        
        if (element.isClickable) {
            clickable.add(element)
        }
        
        element.children.forEach { child ->
            clickable.addAll(collectClickableElements(child))
        }
        
        return clickable
    }
}