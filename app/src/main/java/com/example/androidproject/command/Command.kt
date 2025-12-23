package com.example.androidproject.command

sealed class Command {
    data class Tap(val x: Float, val y: Float) : Command()
    data class Swipe(val startX: Float, val startY: Float, val endX: Float, val endY: Float, val duration: Long = 300) : Command()
    data class Scroll(val direction: ScrollDirection, val duration: Long = 500) : Command()
    data class InputText(val text: String) : Command()
    data class Wait(val duration: Long) : Command()
    object Back : Command()
    object Home : Command()
    object RecentApps : Command()
    
    enum class ScrollDirection {
        UP, DOWN, LEFT, RIGHT
    }
}