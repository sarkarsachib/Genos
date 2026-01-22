# GENOS Package Structure Documentation

## Overview

GENOS follows a modular architecture with clear separation of concerns. Each module is responsible for specific functionality and communicates through well-defined interfaces.

## Package Hierarchy

```
ai.genos.core/
├── GenosApplication.kt                    # Application entry point
├── GenosBootReceiver.kt                   # Boot completed receiver
│
├── accessibility/                             # Accessibility & Input Emulation
│   └── GenosAccessibilityService.kt        # Main accessibility service
│       ├── Gesture emulation (tap, swipe)
│       ├── Node discovery (find by text/content)
│       └── Action execution (click, scroll, type)
│
├── vision/                                  # Screen Capture & Vision Analysis
│   ├── ScreenCaptureService.kt              # MediaProjection service
│   ├── ocr/                                # Text recognition (placeholder)
│   └── pose/                                # Pose detection (placeholder)
│
├── audio/                                   # Audio Processing (placeholder)
│   ├── SpeechRecognition.kt                 # Speech-to-text
│   └── AudioManager.kt                    # Audio capture
│
├── sensors/                                  # Device Sensor Monitoring
│   └── SensorListenerService.kt              # Sensor monitoring service
│       ├── Accelerometer
│       ├── Gyroscope
│       └── Proximity
│
├── ai/                                      # AI Integration
│   ├── gemini/                            # Gemini API Client
│   │   └── GeminiClient.kt                 # API communication
│   └── models/                             # Data Models
│       ├── GenosAction.kt                  # Single action model
│       └── ActionPlan.kt                   # Action sequence model
│
├── execution/                                # Command Execution
│   └── CommandExecutorService.kt            # Execute action plans
│       ├── Parse action plan
│       ├── Execute individual actions
│       └── Handle completion/errors
│
├── privilege/                                # Privilege Escalation (optional)
│   └── ShizukuManager.kt                  # Shizuku integration
│       ├── System commands
│       └── Direct shell access
│
├── state/                                   # State Management
│   ├── GenosStateManager.kt                # Global state manager
│   └── Preferences.kt                      # User preferences
│
└── ui/                                      # User Interface
    ├── MainActivity.kt                      # Main activity
    ├── PermissionRequestActivity.kt           # Permission requests
    ├── SetupActivity.kt                    # Onboarding
    └── OverlayService.kt                   # Floating HUD
```

## Module Responsibilities

### Accessibility Module

**Purpose**: Enable rootless input emulation and UI interaction

**Key Components**:
- `GenosAccessibilityService`: Main service for accessibility features
- Gesture dispatching via `dispatchGesture()`
- Node discovery via AccessibilityNodeInfo tree
- Action execution via `performAction()`

**Key Functions**:
- `performTap(x, y)`: Execute tap at coordinates
- `performSwipe(start, end, duration)`: Execute swipe gesture
- `findNodeByText(text)`: Find UI elements by text
- `clickNode(node)`: Click on accessibility node
- `setNodeText(node, text)`: Input text into editable fields
- `getScreenContent()`: Extract screen as text

**Permissions Required**:
- `BIND_ACCESSIBILITY_SERVICE`

### Vision Module

**Purpose**: Capture screen content for analysis

**Key Components**:
- `ScreenCaptureService`: MediaProjection API integration
- ImageReader for frame capture
- Bitmap conversion for ML Kit

**Key Functions**:
- `startCapture(resultCode, data)`: Start screen capture
- `stopCapture()`: Stop screen capture
- `captureScreen()`: Capture single frame
- `imageToBitmap(image)`: Convert Image to Bitmap

**Permissions Required**:
- `FOREGROUND_SERVICE_MEDIA_PROJECTION`

### AI Module

**Purpose**: Generate action plans using Gemini AI

**Key Components**:
- `GeminiClient`: Retrofit-based API client
- `ActionPlan`: Sequence of actions
- `GenosAction`: Individual action with parameters

**Key Functions**:
- `generateActionPlan(screenContent, instruction)`: Create plan from screen + instruction
- `executeAction(action)`: Execute single action
- `parseActionPlan(json)`: Parse AI response

**Configuration**:
- Endpoint: `https://generativelanguage.googleapis.com`
- Model: `gemini-2.0-flash`
- API Key: Via BuildConfig

### Execution Module

**Purpose**: Execute action plans safely

**Key Components**:
- `CommandExecutorService`: Foreground service for execution
- Coroutine-based action queue
- Error handling and retry logic

**Key Functions**:
- `executeActionPlan(plan)`: Execute entire plan
- `executeAction(action)`: Execute single action
- `stopExecution()`: Cancel running execution

**Action Types**:
- `TAP`: Tap at coordinates
- `SWIPE`: Swipe gesture
- `TEXT`: Input text
- `SCROLL`: Scroll screen
- `LAUNCH`: Launch app
- `WAIT`: Wait for duration

### UI Module

**Purpose**: User interface and overlay display

**Key Components**:
- `MainActivity`: Status dashboard
- `PermissionRequestActivity`: Permission guidance
- `SetupActivity`: Onboarding flow
- `OverlayService`: Floating HUD

**Key Functions**:
- Service status monitoring
- Permission request handling
- Overlay toggle
- Real-time status updates

### Sensors Module

**Purpose**: Device context awareness

**Key Components**:
- `SensorListenerService`: Sensor monitoring
- Accelerometer, Gyroscope, Proximity

**Key Functions**:
- Sensor event listening
- Orientation detection
- Proximity awareness

## Data Flow

### Action Plan Execution Flow

```
1. User provides instruction
   ↓
2. ScreenCaptureService captures screen
   ↓
3. GeminiClient generates action plan
   ↓
4. CommandExecutorService receives plan
   ↓
5. Execute each action sequentially
   ↓
6. GenosAccessibilityService performs gestures
   ↓
7. OverlayService updates status
```

### Service Communication

Services communicate via:
- **Intents**: For service start/stop commands
- **Callbacks**: For result delivery
- **BroadcastReceivers**: For event notifications
- **Singleton instances**: For direct method calls

## Concurrency Model

### Main Thread
- UI updates
- Accessibility node queries
- Service start/stop

### Background Threads
- Screen capture
- AI API calls
- Action execution queue
- Sensor monitoring

### Coroutines
- Asynchronous action execution
- Network requests
- Delay handling

## Architecture Patterns

### Singleton Pattern
Services maintain singleton instances for easy access:
```kotlin
companion object {
    @Volatile
    private var instance: GenosAccessibilityService? = null

    fun getInstance(): GenosAccessibilityService? = instance
}
```

### Repository Pattern
Data access abstracted through repository classes (future implementation)

### MVVM Pattern
UI components follow Model-View-ViewModel pattern (future implementation)

### Observer Pattern
Services observe each other's state via listeners:
```kotlin
fun setOnScreenCapturedListener(listener: (Bitmap) -> Unit) {
    onScreenCapturedListener = listener
}
```

## Dependency Injection

Current: Manual instantiation
Future: Consider Hilt/Koin for DI

## Error Handling

### Service Level
- Try-catch blocks around critical operations
- Result type for API calls
- Error logging via Timber

### User Level
- Toast notifications for errors
- Overlay status updates
- Graceful degradation

## Security Considerations

### Sensitive Permissions
- Accessibility: User explicitly enables
- Screen capture: User explicitly grants
- All permissions declared in manifest

### Data Privacy
- No data uploaded without consent
- API key stored in BuildConfig
- Logs stripped in release builds

## Testing Strategy

### Unit Tests
- Individual class logic
- Action plan parsing
- JSON serialization

### Instrumented Tests
- Service interaction
- Permission handling
- UI flow

### Manual Testing
- Accessibility service enablement
- Overlay display
- Demo action execution

## Future Enhancements

### Phase 2
- Room database for action history
- Settings persistence
- Action plan templates
- Custom action plans from UI

### Phase 3
- Vision analysis integration
- OCR text extraction
- Object detection
- Gesture recognition from sensors

### Phase 4
- Multi-action plans
- Conditional execution
- Loop constructs
- Error recovery

## Build Variants

### Debug
- Timber debug logging
- ProGuard disabled
- `.debug` application suffix

### Release
- Timber release logging (errors only)
- ProGuard enabled
- Shrunk resources
- Signed APK

## Configuration

### BuildConfig Fields
- `GEMINI_API_KEY`: API authentication
- `GEMINI_ENDPOINT`: API base URL
- `GEMINI_MODEL`: AI model to use
- `DEBUG_MODE`: Build type flag

### Gradle Properties
- JVM args for memory
- AndroidX enabled
- Jetifier enabled
- Kotlin version

## Extension Points

### Adding New Action Types
1. Add to `GenosAction.ActionType` enum
2. Implement in `CommandExecutorService.executeAction()`
3. Add to Gemini prompt
4. Update ProGuard rules

### Adding New Services
1. Create service class
2. Declare in AndroidManifest.xml
3. Add to package structure
4. Document in this file

### Adding New Sensors
1. Add to `SensorListenerService`
2. Update manifest permissions
3. Add sensor type
4. Implement callback handling

## Best Practices

1. **Code Organization**: Keep modules focused
2. **Naming**: Clear, descriptive names
3. **Documentation**: KDoc for all public APIs
4. **Testing**: Unit + integration tests
5. **Logging**: Use Timber for all logs
6. **Error Handling**: Never crash silently
7. **Threading**: Keep main thread responsive
8. **Memory**: Release resources promptly
9. **Permissions**: Request minimally, explain clearly
10. **User Experience**: Provide feedback on all actions
