package com.example.androidproject.command

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.graphics.Rect
import android.util.Log

class CommandExecutor(private val accessibilityService: AccessibilityService) {
    
    private val commandProcessor = CommandProcessor()
    
    suspend fun executeCommand(command: Command): Boolean {
        return try {
            when (command) {
                is Command.Tap -> executeTap(command.x, command.y)
                is Command.Swipe -> executeSwipe(command.startX, command.startY, command.endX, command.endY, command.duration)
                is Command.Scroll -> executeScroll(command.direction, command.duration)
                is Command.InputText -> executeInputText(command.text)
                is Command.Wait -> executeWait(command.duration)
                Command.Back -> executeBack()
                Command.Home -> executeHome()
                Command.RecentApps -> executeRecentApps()
            }
            Log.d("CommandExecutor", "Successfully executed: ${commandProcessor.formatCommandForDisplay(command)}")
            true
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Failed to execute command: $command", e)
            false
        }
    }
    
    suspend fun executeCommandString(commandString: String): Boolean {
        val command = commandProcessor.parseCommand(commandString) ?: return false
        return executeCommand(command)
    }
    
    suspend fun executeCommands(commands: List<Command>): Boolean {
        for (command in commands) {
            if (!executeCommand(command)) {
                return false // Stop on first failure
            }
            // Brief delay between commands
            delay(500)
        }
        return true
    }
    
    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun executeTap(x: Float, y: Float): Boolean = withContext(Dispatchers.Main) {
        try {
            val path = Path().apply {
                moveTo(x, y)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 10))
                .build()
            
            val result = accessibilityService.dispatchGesture(gesture, null, null)
            delay(100) // Wait for gesture to complete
            result
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Error executing tap at ($x, $y)", e)
            false
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun executeSwipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long): Boolean = withContext(Dispatchers.Main) {
        try {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()
            
            val result = accessibilityService.dispatchGesture(gesture, null, null)
            delay(duration + 100) // Wait for gesture to complete
            result
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Error executing swipe from ($startX, $startY) to ($endX, $endY)", e)
            false
        }
    }
    
    private suspend fun executeScroll(direction: Command.ScrollDirection, duration: Long): Boolean = withContext(Dispatchers.Main) {
        try {
            val rootNode = accessibilityService.rootInActiveWindow ?: return@withContext false
            
            // Find a scrollable node
            val scrollableNode = findScrollableNode(rootNode, direction)
            if (scrollableNode != null) {
                val result = when (direction) {
                    Command.ScrollDirection.UP -> scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                    Command.ScrollDirection.DOWN -> scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    Command.ScrollDirection.LEFT -> scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                    Command.ScrollDirection.RIGHT -> scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                }
                delay(duration)
                result
            } else {
                // Fallback to gesture-based scroll
                val displayMetrics = accessibilityService.resources.displayMetrics
                val centerX = displayMetrics.widthPixels / 2f
                val centerY = displayMetrics.heightPixels / 2f
                val scrollDistance = 500f
                
                val (startX, startY, endX, endY) = when (direction) {
                    Command.ScrollDirection.UP -> Pair(centerX, centerY + scrollDistance, centerX, centerY - scrollDistance)
                    Command.ScrollDirection.DOWN -> Pair(centerX, centerY - scrollDistance, centerX, centerY + scrollDistance)
                    Command.ScrollDirection.LEFT -> Pair(centerX + scrollDistance, centerY, centerX - scrollDistance, centerY)
                    Command.ScrollDirection.RIGHT -> Pair(centerX - scrollDistance, centerY, centerX + scrollDistance, centerY)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    executeSwipe(startX, startY, endX, endY, duration)
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Error executing scroll $direction", e)
            false
        }
    }
    
    private fun findScrollableNode(node: AccessibilityNodeInfo, direction: Command.ScrollDirection): AccessibilityNodeInfo? {
        // Check if current node is scrollable
        if (node.isScrollable) {
            return node
        }
        
        // Recursively check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val scrollable = findScrollableNode(child, direction)
                if (scrollable != null) {
                    return scrollable
                }
            }
        }
        
        return null
    }
    
    private suspend fun executeInputText(text: String): Boolean = withContext(Dispatchers.Main) {
        try {
            val rootNode = accessibilityService.rootInActiveWindow ?: return@withContext false
            val focusedNode = findFocusedNode(rootNode)
            
            if (focusedNode != null && focusedNode.isEditable) {
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                delay(500)
                result
            } else {
                Log.w("CommandExecutor", "No focused editable node found for text input")
                false
            }
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Error executing text input: $text", e)
            false
        }
    }
    
    private fun findFocusedNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val focused = findFocusedNode(child)
                if (focused != null) {
                    return focused
                }
            }
        }
        
        return null
    }
    
    private suspend fun executeWait(duration: Long): Boolean {
        delay(duration)
        return true
    }
    
    private suspend fun executeBack(): Boolean = withContext(Dispatchers.Main) {
        try {
            val result = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            delay(300)
            result
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Error executing back action", e)
            false
        }
    }
    
    private suspend fun executeHome(): Boolean = withContext(Dispatchers.Main) {
        try {
            val result = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            delay(300)
            result
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Error executing home action", e)
            false
        }
    }
    
    private suspend fun executeRecentApps(): Boolean = withContext(Dispatchers.Main) {
        try {
            val result = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            delay(300)
            result
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Error executing recent apps action", e)
            false
        }
    }
}