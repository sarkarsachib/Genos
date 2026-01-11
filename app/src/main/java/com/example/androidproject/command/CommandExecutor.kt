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
    
    /**
     * Executes a single Command by dispatching to the appropriate handler and logging the result.
     *
     * @param command The command to execute.
     * @return `true` if execution completed without throwing an exception, `false` otherwise.
     */
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
    
    /**
     * Parses a command string and executes the resulting command.
     *
     * @param commandString A command encoded as a string that CommandProcessor can parse.
     * @return `true` if the parsed command executed successfully, `false` if parsing failed or execution failed.
     */
    suspend fun executeCommandString(commandString: String): Boolean {
        val command = commandProcessor.parseCommand(commandString) ?: return false
        return executeCommand(command)
    }
    
    /**
     * Executes a sequence of commands in order, stopping on the first failure.
     *
     * Each command is executed sequentially with a short (500 ms) delay between commands.
     *
     * @param commands The list of commands to execute in order.
     * @return `true` if every command in the list executed successfully, `false` if execution stopped due to a failed command.
     */
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
    
    /**
     * Performs a short tap gesture at the specified screen coordinates.
     *
     * @param x The x coordinate on the screen in pixels.
     * @param y The y coordinate on the screen in pixels.
     * @return `true` if the gesture was dispatched successfully, `false` otherwise.
     */
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
    
    /**
     * Performs a swipe gesture from the given start coordinates to the end coordinates over the specified duration.
     *
     * @param startX The starting X coordinate in screen pixels.
     * @param startY The starting Y coordinate in screen pixels.
     * @param endX The ending X coordinate in screen pixels.
     * @param endY The ending Y coordinate in screen pixels.
     * @param duration The gesture duration in milliseconds.
     * @return `true` if the gesture was dispatched successfully, `false` otherwise.
     */
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
    
    /**
     * Performs a scroll in the given direction for the specified duration, using a scrollable node when available and falling back to a gesture-based swipe if necessary.
     *
     * @param direction The direction to scroll (UP, DOWN, LEFT, RIGHT).
     * @param duration The duration of the scroll in milliseconds.
     * @return `true` if the scroll action or fallback gesture was successfully performed, `false` otherwise.
     */
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
    
    /**
     * Searches the provided node's subtree for a scrollable accessibility node.
     *
     * @param node The root AccessibilityNodeInfo to start the search from.
     * @param direction The desired scroll direction to consider when locating a scrollable node (currently not used to influence the search).
     * @return The first scrollable AccessibilityNodeInfo found in the subtree, or `null` if none is found.
     */
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
    
    /**
     * Inputs the given text into the currently focused editable accessibility node, if one exists.
     *
     * Attempts to find the focused editable node in the active window and set its text to `text`.
     *
     * @param text The text to insert into the focused editable node.
     * @return `true` if the text was successfully set on a focused editable node, `false` otherwise.
     */
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
    
    /**
     * Finds the currently focused AccessibilityNodeInfo within the subtree rooted at the given node.
     *
     * @param node The root node to search for a focused descendant.
     * @return The focused node if one exists in the subtree, `null` otherwise.
     */
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
    
    /**
     * Pauses execution for the specified duration.
     *
     * @param duration Duration to wait in milliseconds.
     * @return `true` after the pause completes.
     */
    private suspend fun executeWait(duration: Long): Boolean {
        delay(duration)
        return true
    }
    
    /**
     * Triggers the system Back action and waits briefly for the action to complete.
     *
     * @return `true` if the global back action was performed successfully, `false` otherwise.
     */
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
    
    /**
     * Triggers the system Home action and waits briefly for the action to complete.
     *
     * @return `true` if the global Home action was dispatched successfully, `false` otherwise.
     */
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
    
    /**
     * Opens the system recent apps (overview) using the AccessibilityService.
     *
     * @return `true` if the global recents action was performed, `false` otherwise.
     */
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