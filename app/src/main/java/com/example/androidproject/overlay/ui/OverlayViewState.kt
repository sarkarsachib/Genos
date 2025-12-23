package com.example.androidproject.overlay.ui

data class OverlayViewState(
    val genosStatus: String = "GENOS: Idle",
    val currentApp: String = "No app detected",
    val isAutomationRunning: Boolean = false,
    val showUiTree: Boolean = false,
    val uiTree: String = "",
    val lastAction: String = "",
    val touchVisualization: TouchVisualization? = null,
    val ocrText: String = ""
)

data class TouchVisualization(
    val x: Float,
    val y: Float,
    val type: TouchType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TouchType {
    TAP,
    SWIPE_START,
    SWIPE_END,
    SCROLL
}