package com.example.androidproject.command

import android.util.Log

class CommandProcessor {
    
    fun parseCommand(input: String): Command? {
        val tokens = input.trim().split("\s+")
        if (tokens.isEmpty()) return null
        
        return try {
            when (tokens[0].lowercase()) {
                "tap" -> {
                    if (tokens.size < 3) return null
                    val x = tokens[1].toFloatOrNull() ?: return null
                    val y = tokens[2].toFloatOrNull() ?: return null
                    Command.Tap(x, y)
                }
                
                "swipe" -> {
                    if (tokens.size < 5) return null
                    val startX = tokens[1].toFloatOrNull() ?: return null
                    val startY = tokens[2].toFloatOrNull() ?: return null
                    val endX = tokens[3].toFloatOrNull() ?: return null
                    val endY = tokens[4].toFloatOrNull() ?: return null
                    val duration = if (tokens.size > 5) tokens[5].toLongOrNull() ?: 300 else 300
                    Command.Swipe(startX, startY, endX, endY, duration)
                }
                
                "scroll" -> {
                    if (tokens.size < 2) return null
                    val direction = when (tokens[1].uppercase()) {
                        "UP" -> Command.ScrollDirection.UP
                        "DOWN" -> Command.ScrollDirection.DOWN
                        "LEFT" -> Command.ScrollDirection.LEFT
                        "RIGHT" -> Command.ScrollDirection.RIGHT
                        else -> return null
                    }
                    val duration = if (tokens.size > 2) tokens[2].toLongOrNull() ?: 500 else 500
                    Command.Scroll(direction, duration)
                }
                
                "input" -> {
                    if (tokens.size < 2) return null
                    val text = tokens.drop(1).joinToString(" ")
                    Command.InputText(text)
                }
                
                "wait" -> {
                    if (tokens.size < 2) return null
                    val duration = tokens[1].toLongOrNull() ?: return null
                    Command.Wait(duration)
                }
                
                "back" -> Command.Back
                "home" -> Command.Home
                "recents" -> Command.RecentApps
                else -> null
            }
        } catch (e: Exception) {
            Log.e("CommandProcessor", "Error parsing command: $input", e)
            null
        }
    }
    
    fun parseCommands(commands: String): List<Command> {
        val lines = commands.lines()
        val parsedCommands = mutableListOf<Command>()
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                parseCommand(trimmed)?.let { parsedCommands.add(it) }
            }
        }
        
        return parsedCommands
    }
    
    fun formatCommandForDisplay(command: Command): String {
        return when (command) {
            is Command.Tap -> "Tap at (${command.x.toInt()}, ${command.y.toInt()})"
            is Command.Swipe -> "Swipe from (${command.startX.toInt()}, ${command.startY.toInt()}) to (${command.endX.toInt()}, ${command.endY.toInt()})"
            is Command.Scroll -> "Scroll ${command.direction.name}"
            is Command.InputText -> "Input: \"${command.text}\""
            is Command.Wait -> "Wait ${command.duration}ms"
            Command.Back -> "Press Back"
            Command.Home -> "Press Home"
            Command.RecentApps -> "Show Recent Apps"
        }
    }
}