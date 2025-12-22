package com.genos.accessibility.model

import android.graphics.Rect
import android.os.Parcelable
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.parcelize.Parcelize

@Parcelize
@kotlinx.serialization.Serializable
data class UiElement(
    val id: String? = null,
    val className: String? = null,
    val packageName: String? = null,
    val text: String? = null,
    val contentDescription: String? = null,
    val viewIdResourceName: String? = null,
    val bounds: UiBounds? = null,
    val boundsInScreen: UiBounds? = null,
    val isClickable: Boolean = false,
    val isEnabled: Boolean = true,
    val isFocusable: Boolean = false,
    val isFocused: Boolean = false,
    val isScrollable: Boolean = false,
    val isSelected: Boolean = false,
    val isVisible: Boolean = true,
    val children: List<UiElement> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
@kotlinx.serialization.Serializable
data class UiBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) : Parcelable {
    constructor(rect: Rect) : this(rect.left, rect.top, rect.right, rect.bottom)
}

@Parcelize
@kotlinx.serialization.Serializable
data class UiTree(
    val root: UiElement,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

fun AccessibilityNodeInfo.toUiElement(): UiElement {
    val rect = Rect()
    val screenRect = Rect()
    
    getBoundsInParent(rect)
    getBoundsInScreen(screenRect)
    
    val childrenList = mutableListOf<UiElement>()
    for (i in 0 until childCount) {
        getChild(i)?.toUiElement()?.let { childrenList.add(it) }
    }
    
    return UiElement(
        id = viewIdResourceName,
        className = className?.toString(),
        packageName = packageName,
        text = text?.toString(),
        contentDescription = contentDescription?.toString(),
        viewIdResourceName = viewIdResourceName,
        bounds = UiBounds(rect),
        boundsInScreen = UiBounds(screenRect),
        isClickable = isClickable,
        isEnabled = isEnabled,
        isFocusable = isFocusable,
        isFocused = isFocused,
        isScrollable = isScrollable,
        isSelected = isSelected,
        isVisible = isVisibleToUser,
        children = childrenList
    )
}