# Input Emulation Engine - Usage Guide

## Overview
The Input Emulation Engine provides rootless automation capabilities for Android through the Accessibility Service API. It supports taps, swipes, scrolls, and text input with proper callbacks and safety validation.

## Architecture

### Core Components
- **InputExecutor**: Main interface for executing commands
- **InputExecutorImpl**: Implementation using AccessibilityService gestures
- **MyAccessibilityService**: Accessibility service with gesture dispatch capabilities

### Command Types
```kotlin
sealed class InputCommand {
    data class TapCommand(val points: List<Point>, val durationMs: Long)
    data class SwipeCommand(val startX: Int, val startY: Int, val endX: Int, val endY: Int, val durationMs: Long)
    data class ScrollCommand(val deltaX: Int, val deltaY: Int, val x: Int, val y: Int, val scrollDurationMs: Long)
    data class TypeCommand(val text: String, val clearExisting: Boolean, val commitIme: Boolean)
}
```

## Setup Instructions

### 1. Enable Accessibility Service
Navigate to:
```
Settings → Accessibility → My Accessibility Service → Enable
```

### 2. Configure Service Permissions
The service is pre-configured with:
```xml
android:canPerformGestures="true"
android:canRetrieveWindowContent="true"
```

## Usage Examples

### Basic Tap
```kotlin
// Single tap at coordinates (500, 800)
service.executeInputCommand(
    InputCommand.TapCommand(500, 800)
) { result ->
    when (result) {
        is InputResult.Success -> Log.d("Tap", "Completed: ${result.message}")
        is InputResult.Failure -> Log.e("Tap", "Failed: ${result.reason}")
    }
}

// Long press (500ms)
service.executeInputCommand(
    InputCommand.TapCommand(500, 800, durationMs = 500)
) { result ->
    // Handle result
}
```

### Multi-Point Tap
```kotlin
// Simultaneous taps at multiple points
val points = listOf(
    Point(100, 100),
    Point(200, 200),
    Point(300, 300)
)
service.executeInputCommand(
    InputCommand.TapCommand(points, durationMs = 100)
) { result ->
    // All points tapped simultaneously
}
```

### Swipe Gesture
```kotlin
// Fast swipe from (100, 1000) to (100, 500)
service.executeInputCommand(
    InputCommand.SwipeCommand(100, 1000, 100, 500, durationMs = 200)
) { result ->
    // Swipe completed
}

// Slow, controlled swipe
service.executeInputCommand(
    InputCommand.SwipeCommand(0, 800, 1000, 800, durationMs = 1000)
) { result ->
    // Smooth horizontal swipe
}
```

### Scroll Gesture
```kotlin
// Scroll down by 300 pixels
service.executeInputCommand(
    InputCommand.ScrollCommand(deltaX = 0, deltaY = -300, x = 500, y = 800)
) { result ->
    // Scroll completed
}

// Scroll up (positive delta)
service.executeInputCommand(
    InputCommand.ScrollCommand(deltaX = 0, deltaY = 300)
) { result ->
    // Scroll up completed
}

// Horizontal scroll
service.executeInputCommand(
    InputCommand.ScrollCommand(deltaX = 500, deltaY = 0)
) { result ->
    // Horizontal scroll completed
}
```

### Text Input
```kotlin
// Type into currently focused field
service.executeInputCommand(
    InputCommand.TypeCommand("Hello, World!")
) { result ->
    when (result) {
        is InputResult.Success -> {
            val metadata = result.metadata
            Log.d("Type", "Text length: ${metadata?.get("textLength")}")
        }
        is InputResult.Failure -> {
            Log.e("Type", "Failed: ${result.reason}")
        }
    }
}

// Clear existing text and type new text
service.executeInputCommand(
    InputCommand.TypeCommand("New text", clearExisting = true)
) { result ->
    // Field cleared and new text entered
}

// Type and submit (simulates Enter key)
service.executeInputCommand(
    InputCommand.TypeCommand("Search query", commitIme = true)
) { result ->
    // Text entered and submitted
}
```

## Testing

### Debug Harness
Launch the debug harness activity to test all commands interactively:
```kotlin
val intent = Intent(context, InputDebugHarnessActivity::class.java)
startActivity(intent)
```

The harness provides:
- Tap coordinate input fields
- Swipe start/end configuration
- Scroll delta controls
- Text input testing
- Real-time status updates

### Instrumentation Tests
Run the included tests to verify functionality:
```bash
./gradlew connectedAndroidTest
```

Test coverage includes:
- Command model validation
- Result handling
- Coordinate validation
- Service availability

## Error Handling

The engine provides detailed error types:

```kotlin
sealed class InputResult {
    data class Success(val message: String, val metadata: Map<String, Any>?)
    data class Failure(val reason: String, val error: Throwable?, val errorType: ErrorType)
}

enum class ErrorType {
    PERMISSION_DENIED,           // Accessibility service not enabled
    SERVICE_NOT_AVAILABLE,       // Service not ready
    INVALID_COORDINATES,         // Coordinates out of bounds
    FOCUS_NOT_FOUND,            // No focused input field
    INPUT_METHOD_NOT_ACTIVE,    // IME not available
    GESTURE_DISPATCH_FAILED,    // Gesture dispatch error
    TIMEOUT,                    // Operation timeout
    INVALID_STATE,              // Invalid service state
    BOUNDARY_VIOLATION          // Coordinate boundary violation
}
```

## Safety Features

### Coordinate Validation
- All coordinates validated against screen bounds
- Prevents out-of-bounds gestures
- Returns detailed error messages

### Concurrency Protection
- Prevents multiple simultaneous commands
- Maintains execution state
- Thread-safe implementation

### Service State Monitoring
- Validates service readiness before execution
- Checks window content availability
- Proper lifecycle management

## Performance Considerations

### Gesture Timing
- Default tap duration: 100ms
- Default swipe duration: 300ms
- Default scroll duration: 200ms
- Customizable per command

### Callback Handling
- All callbacks executed on main thread
- Non-blocking execution
- Proper exception handling

### Memory Management
- Weak references to prevent leaks
- Automatic resource cleanup
- Efficient bitmap handling

## Limitations

- Requires user to enable accessibility service
- Cannot interact with secure system dialogs
- Text input requires focused input field
- Coordinates must be within screen bounds
- Single command execution at a time

## Troubleshooting

### "Service not ready" error
- Ensure accessibility service is enabled
- Check app has all required permissions
- Verify device supports accessibility gestures

### "Invalid coordinates" error
- Validate coordinates are within screen bounds
- Check orientation matches coordinate system
- Ensure coordinates are positive integers

### "Focus not found" error
- Tap the input field to focus it first
- Verify the field is editable
- Check if field accepts text input

### Gesture failures
- Try longer durations for complex gestures
- Ensure path is valid and continuous
- Check for system UI overlays interfering

## Integration Examples

### Automation Script
```kotlin
class AutomationScript(private val service: MyAccessibilityService) {
    
    fun login(username: String, password: String) {
        // Tap username field
        service.executeInputCommand(InputCommand.TapCommand(500, 600)) { 
            // Wait for field focus
            Thread.sleep(500)
            
            // Type username
            service.executeInputCommand(InputCommand.TypeCommand(username)) {
                // Tap password field
                service.executeInputCommand(InputCommand.TapCommand(500, 800)) {
                    Thread.sleep(500)
                    
                    // Type password
                    service.executeInputCommand(InputCommand.TypeCommand(password)) {
                        // Tap login button
                        service.executeInputCommand(InputCommand.TapCommand(500, 1000)) {
                            Log.d("Automation", "Login script completed")
                        }
                    }
                }
            }
        }
    }
}
```

### Robot Pattern
```kotlin
class AppRobot(private val service: MyAccessibilityService) {
    
    fun tapButton(x: Int, y: Int): AppRobot {
        service.executeInputCommand(InputCommand.TapCommand(x, y)) {}
        Thread.sleep(300) // Wait for UI response
        return this
    }
    
    fun typeText(text: String): AppRobot {
        service.executeInputCommand(InputCommand.TypeCommand(text)) {}
        Thread.sleep(500) // Wait for text entry
        return this
    }
    
    fun scrollDown(): AppRobot {
        service.executeInputCommand(InputCommand.ScrollCommand(0, -500)) {}
        Thread.sleep(400) // Wait for scroll animation
        return this
    }
}

// Usage
val robot = AppRobot(service)
robot
    .tapButton(500, 600)
    .typeText("example@email.com")
    .tapButton(500, 800)
    .typeText("password123")
    .tapButton(500, 1000)
```

## Version History

- **v1.0.0**: Initial implementation with all acceptance criteria met