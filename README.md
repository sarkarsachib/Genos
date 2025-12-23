# Screen Capture and OCR Android Application

This Android application implements a comprehensive screen capture and OCR pipeline that leverages the MediaProjection API for screen capture, ML Kit for OCR processing, accessibility tree integration for UI element detection, and is ready for Gemini AI integration.

## 🎯 Key Features

- **MediaProjection API Integration**: Real-time screen capture with user consent flow
- **Dual OCR Support**: ML Kit Text Recognition and Tesseract via tess-two
- **Accessibility Tree Integration**: Real-time UI element hierarchy and properties extraction
- **Screen State Aggregation**: Combined payload of screenshot, OCR text, and UI elements
- **Lifecycle Management**: Automatic stopping when screen is off or app is backgrounded
- **Comprehensive Testing**: Unit tests for all core components
# GENOS Accessibility Core Service

A comprehensive Android accessibility service implementation for the GENOS platform that provides UI tree extraction, app context tracking, and event monitoring capabilities.

## Features

### Core Accessibility Service
- **Foreground Service**: Runs persistently with system notification
- **UI Tree Extraction**: Real-time extraction of UI hierarchy using AccessibilityNodeInfo
- **App Context Tracking**: Monitors current package, activity, and window changes
- **Event Monitoring**: Captures accessibility events (clicks, focus, scroll, etc.)
- **Event Bus**: Observer pattern for inter-module communication
- **Command Router**: API for downstream modules to interact with the service

### UI Tree Extraction
- Complete screen hierarchy as structured JSON-like DTOs
- Node bounds, accessibility attributes, and metadata
- Support for nested child nodes
- Efficient tree traversal and serialization

### Context Tracking
- Real-time package name and activity monitoring
- Window state changes detection
- App transition history
- Service lifecycle management

### Event Bus & Observer Pattern
- Accessibility event listeners
- App context change listeners
- UI tree change listeners
- Service state change listeners

### Command Router API
- Tree snapshot requests
- Node finding by text/ID
- Action execution (click, scroll, set text)
- Context queries
- Transition history

## Architecture

```
ai.genos.core.accessibility/
├── GenosAccessibilityService.kt          # Main accessibility service
├── AccessibilityServiceManager.kt        # Service state management
├── CommandRouter.kt                      # Command execution router
├── Models.kt                             # Data models (UiNode, UiTreeSnapshot, etc.)
├── Logger.kt                             # Logging utility
├── PermissionHelper.kt                   # Permission management
├── GenosForegroundService.kt            # Foreground service wrapper
├── MainActivity.kt                      # Demo/control activity
├── GenosAccessibilityApplication.kt     # Application class
├── BootReceiver.kt                      # Boot completed receiver
├── LogActivity.kt                       # Log viewer activity
└── BuildConfig.kt                       # Build configuration
```

## Data Models

### UiNode
Represents a single UI element in the accessibility tree:
- Node ID, class name, resource name
- Content description and text
- Accessibility properties (clickable, focusable, enabled, visible)
- Screen bounds and position
- Accessibility attributes
- Child nodes

### UiTreeSnapshot
Complete snapshot of the current UI state:
- Timestamp
- Package name and activity
- Window title
- Root UI node with full hierarchy

### AppContext
Current application context:
- Package name
- Activity name
- Window title
- Screen state
- Timestamp

### AppTransition
App transition event:
- From/to package and activity
- Timestamp
- Event type

## API Usage

### Getting UI Tree Snapshots
```kotlin
val service = GenosAccessibilityService.getInstance()
val treeSnapshot = service?.getCurrentUiTree()
if (treeSnapshot != null) {
    println("Current package: ${treeSnapshot.packageName}")
    println("UI tree: ${gson.toJson(treeSnapshot)}")
}
```

### Adding Event Listeners
```kotlin
val eventListener = object : AccessibilityEventListener {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        println("Event: ${event.eventType} in ${event.packageName}")
    }
}

service?.addEventListener(eventListener)
```

### Context Monitoring
```kotlin
val contextListener = object : AppContextListener {
    override fun onAppTransition(transition: AppTransition) {
        println("Transition: ${transition.fromPackage} -> ${transition.toPackage}")
    }
}

service?.addContextListener(contextListener)
```

### Executing Commands
```kotlin
val command = AccessibilityCommand(
    type = CommandType.FIND_NODE_BY_TEXT,
    parameters = mapOf("text" to "Login")
)

val result = service?.executeCommand(command)
if (result?.success == true) {
    println("Found nodes: ${result.data}")
}
```

### Service Management
```kotlin
val serviceManager = GenosAccessibilityService.getServiceManager()
val isRunning = serviceManager.isServiceRunning()
val recentTransitions = serviceManager.getRecentTransitions(10)
val stats = serviceManager.getServiceStatistics()
```

## Permission Setup

### Required Permissions
1. **Accessibility Service**: Must be enabled by user in system settings
2. **Foreground Service**: For persistent operation
3. **Overlay Permission**: For certain UI interactions (optional)

### Permission Helper Usage
```kotlin
// Check permissions
val status = PermissionHelper.getPermissionStatus(context)
if (status.allPermissionsGranted) {
    // Start service
} else {
    // Show setup dialog
    PermissionHelper.showAccessibilitySetupDialog(activity)
}

// Request specific permissions
PermissionHelper.requestAccessibilityService(activity)
PermissionHelper.requestOverlayPermission(activity)
```

## Service Configuration

### AndroidManifest.xml
The service is configured in the manifest with:
- Accessibility service intent filter
- Metadata configuration (XML)
- Required permissions
- Foreground service type

### accessibility_service_config.xml
Configuration includes:
- Event types to monitor
- Feedback type
- Flags for window content retrieval
- Notification timeout
- Capabilities (gestures, touch exploration)

## Testing

### Unit Tests
Located in `src/test/java/`:
- Service manager logic
- Data model validation
- Command router functionality
- Event listener interfaces

### Instrumentation Tests
Located in `src/androidTest/java/`:
- Service lifecycle testing
- UI tree extraction
- Tree serialization
- Permission handling
- Event flow verification

### Running Tests
```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest

# All tests
./gradlew check
```

## Service Lifecycle

### Starting the Service
1. User enables accessibility service in system settings
2. App requests foreground service permission
3. Service starts and connects to accessibility framework
4. Service begins monitoring events and extracting UI tree
5. Notification shown indicating active monitoring

### Monitoring Flow
1. Accessibility events received in `onAccessibilityEvent()`
2. Events processed and categorized
3. UI tree extracted when needed
4. Context changes detected and recorded
5. Event listeners notified
6. Transitions added to history

### Stopping the Service
1. User disables accessibility service
2. Service receives interrupt signal
3. Resources cleaned up
4. Service instance cleared
5. Notification removed

## Logging

### File Logging
- Logs stored in app-specific files directory
- Rotation to maintain file size limits
- Multiple log levels (INFO, DEBUG, WARN, ERROR)
- Timestamped entries with context

### Log Viewer
Built-in activity for viewing logs:
- Real-time log display
- Refresh and clear options
- Monospace formatting for readability

## Error Handling

### Service Recovery
- Automatic restart on crashes
- Health monitoring thread
- Graceful degradation on errors

### Permission Handling
- Graceful handling of disabled permissions
- User guidance for enabling services
- Clear error messages and recovery steps

## Security Considerations

- All accessibility data stays local to device
- No external transmission of UI content
- User consent required for accessibility access
- Minimal permission requests
- Secure command execution with validation

## Performance

- Efficient tree traversal algorithms
- Minimal memory allocation for tree extraction
- Background processing for event handling
- Optimized serialization with Gson
- Lazy loading of node attributes

## Future Enhancements

- Gesture simulation capabilities
- Advanced node filtering and search
- Performance metrics and profiling
- Network-based tree synchronization
- Machine learning for UI pattern recognition
- Accessibility analytics and insights
# GENOS Overlay System

A comprehensive Android application implementing a WindowManager-based overlay with input emulation and command execution capabilities.

## Overview

This project implements a sophisticated Android system that provides:

1. **WindowManager Overlay (HUD)** - A persistent floating overlay showing GENOS status, current app, and Gemini decision summary
2. **Input Emulator Service** - Rootless input emulation using AccessibilityService with dispatchGesture for taps, swipes, typing
3. **Command Executor** - Sequential execution of Gemini action plans with error reporting and safeguards
# Genos Core - AI Assistant Framework

An Android application framework for building AI-powered assistants with screen capture, accessibility services, and intelligent input processing.

## Project Overview

Genos Core is designed to provide a foundation for building AI assistants that can:
- Monitor screen content via Accessibility Services
- Capture screenshots using MediaProjection API
- Process text using ML Kit and Tesseract OCR
- Display overlay interfaces for user interaction
- Process and respond to user input

## Requirements

- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Language**: Kotlin (with Java compatibility)
- **Build Tool**: Gradle 8.2.0+
- **Kotlin Version**: 1.9.20

## Building the Project

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK with API 24+ installed

### Build Commands

Build debug APK:
```bash
./gradlew assembleDebug
```

Build release APK:
```bash
./gradlew assembleRelease
```

Install on connected device:
```bash
./gradlew installDebug
```

Run tests:
```bash
./gradlew test
```

## Architecture

### Package Structure

```
ai.genos.core/
├── service/               # Android Services
│   ├── GenosAccessibilityService.kt    # Accessibility service for screen monitoring
│   ├── ScreenCaptureService.kt         # Foreground service for screen capture
│   ├── OverlayService.kt               # Service for managing overlays
│   └── InputProcessingService.kt       # Service for processing user input
│
├── overlay/               # Overlay UI components
│   └── OverlayManager.kt               # Manages overlay windows
│
├── input/                 # Input processing
│   └── InputProcessor.kt               # Processes text and voice input
│
├── capture/               # Screen capture and OCR
│   ├── ScreenCaptureManager.kt         # Handles screen capture via MediaProjection
│   └── TextRecognizer.kt               # OCR using ML Kit and Tesseract
│
└── ui/                    # User interface
    ├── MainActivity.kt                 # Main entry point
    ├── SettingsActivity.kt             # Settings and configuration
    └── theme/                          # Jetpack Compose theme
```

## Key Technologies

### Core Android Components
- **Accessibility Service**: Monitors screen content and UI events
- **MediaProjection**: Captures screen content
- **Foreground Service**: Keeps capture service running

### Jetpack Components
- **Compose**: Modern declarative UI framework
- **Lifecycle**: Lifecycle-aware components
- **WorkManager**: Background task scheduling
- **Room**: Local database (future use)
- **Navigation**: App navigation

### ML/OCR
- **ML Kit Text Recognition**: Google's on-device text recognition
- **Tesseract**: Open-source OCR engine

## Permissions

The app requires the following permissions:

### Runtime Permissions
- `SYSTEM_ALERT_WINDOW`: Display overlay windows
- `FOREGROUND_SERVICE`: Run foreground services
- `FOREGROUND_SERVICE_MEDIA_PROJECTION`: Screen capture in foreground
- `POST_NOTIFICATIONS`: Display notifications (Android 13+)

### Special Permissions
- `BIND_ACCESSIBILITY_SERVICE`: Accessibility service binding
- `WRITE_SECURE_SETTINGS`: System settings modification (placeholder, requires system signature)

## Phase 1 - Initial Setup (Current)

### Completed
✅ Project structure and Gradle configuration
✅ AndroidManifest with all required permissions
✅ Base service implementations (stubs)
✅ Package structure for modules
✅ Jetpack Compose UI setup
✅ Build system configuration

### Next Steps
- [ ] Implement accessibility service event handling
- [ ] Implement MediaProjection screen capture
- [ ] Integrate ML Kit text recognition
- [ ] Integrate Tesseract OCR
- [ ] Implement overlay window management
- [ ] Add permission request flows
- [ ] Implement input processing pipeline

## Development Guidelines

### Code Style
- Follow Kotlin coding conventions
- Use Jetpack Compose for UI
- Leverage coroutines for async operations
- Use dependency injection (future enhancement)

### Testing
- Write unit tests for business logic
- Write instrumentation tests for UI
- Test on devices with API 24+ and 34

### Architecture Patterns
- MVVM pattern for UI components
- Repository pattern for data access
- Service-oriented architecture for background tasks

## Permissions Setup

### Accessibility Service
Users must manually enable the accessibility service:
1. Go to Settings → Accessibility
2. Find "Genos Accessibility Service"
3. Enable the service

### Overlay Permission
Users must grant overlay permission:
1. Go to Settings → Apps → Genos Core → Display over other apps
2. Enable "Allow display over other apps"

### Screen Capture
Screen capture requires user consent at runtime via MediaProjection API.

## Troubleshooting

### Build Issues
- Ensure you have JDK 17 installed
- Run `./gradlew clean` before building
- Sync Gradle files in Android Studio

### Service Issues
- Check that all permissions are granted
- Verify services are declared in AndroidManifest.xml
- Check logcat for service lifecycle events

## License

[Add your license information here]

## Contributing

[Add contribution guidelines here]
# Gemini AI Client

A Kotlin-based client for interacting with Gemini AI services using Retrofit and OkHttp.

## Features

- **Retrofit-based API client** with OkHttp integration
- **Exponential backoff** for rate limiting and retry logic
- **API key interceptor** for secure authentication
- **Request/response compression** for large payloads (screenshots, node trees)
- **Typed action descriptors** (Tap, Swipe, Type actions)
- **Health check** functionality
- **Configuration management** (BuildConfig, environment variables, properties files)
- **Comprehensive unit tests** with MockK
- **Coroutines support** for asynchronous operations

## Module Structure

```
ai/gemini/
├── src/main/kotlin/com/example/ai/gemini/
│   ├── api/                  # API service and client
│   ├── config/               # Configuration management
│   ├── di/                   # Dependency injection
│   ├── models/               # Request/response models
│   ├── repository/           # Repository layer
│   ├── serialization/        # Compression utilities
│   └── sample/               # Usage examples
└── src/test/kotlin/com/example/ai/gemini/
    ├── api/                  # API tests
    ├── repository/           # Repository tests
    ├── integration/          # Integration tests
    └── test/                 # Test utilities
```

## Models

### Request Models

- `ScreenState`: Contains OCR text, node tree JSON, and optional screenshot
- `ActionRequest`: Main request payload with screen state and instruction
- `RequestConfig`: Configuration for AI parameters (temperature, max tokens, etc.)

### Response Models

- `ActionResponse`: Contains list of actions, confidence, reasoning, and model version
- `ActionDescriptor`: Sealed class with concrete implementations:
  - `TapAction`: Tap gesture with coordinates
  - `SwipeAction`: Swipe gesture with start/end coordinates
  - `TypeAction`: Text input action
- `ErrorResponse`: Standardized error format

## Usage

### Basic Setup

```kotlin
// Initialize configuration
GeminiConfig.baseUrl = "https://api.gemini.example.com/"
GeminiConfig.apiKey = "your-api-key"

// Create client and repository
val geminiClient = GeminiModule.provideGeminiClient()
val geminiRepository = GeminiModule.provideGeminiRepository(geminiClient)
```

### Plan Actions

```kotlin
val screenState = ScreenState(
    ocrText = "Login Screen",
    nodeTreeJson = "{\"nodes\": [...]}",
    screenshotBase64 = "base64-encoded-image"
)

val instruction = "Login with username and password"

val result = geminiRepository.planActions(screenState, instruction)

if (result.isSuccess) {
    val actions = result.getOrNull()
    actions?.forEach { action ->
        when (action) {
            is TapAction -> performTap(action.x, action.y)
            is SwipeAction -> performSwipe(action.startX, action.startY, action.endX, action.endY)
            is TypeAction -> performType(action.text)
        }
    }
}
```

### With Compression

```kotlin
val result = geminiRepository.planActionsWithCompression(screenState, instruction)
```

### Health Check

```kotlin
val healthResult = geminiRepository.healthCheck()
if (healthResult.isSuccess && healthResult.getOrNull() == true) {
    println("Service is healthy")
}
```

## Configuration

### BuildConfig

```kotlin
object GeminiConfig {
    object BuildConfig {
        const val DEBUG = true
        const val GEMINI_BASE_URL = "https://api.gemini.example.com/"
        const val GEMINI_API_KEY = "default-api-key"
    }
}
```

### Environment Variables

```bash
export GEMINI_BASE_URL="https://api.gemini.example.com/"
export GEMINI_API_KEY="your-api-key"
```

### Properties File

Create `gemini.properties` in assets:

```properties
gemini.baseUrl=https://api.gemini.example.com/
gemini.apiKey=your-api-key
gemini.timeoutSeconds=30
gemini.enableLogging=true
gemini.enableCompression=true
```

## Error Handling

The client uses Kotlin's `Result` type for error handling:

```kotlin
val result = geminiRepository.planActions(screenState, instruction)

if (result.isFailure) {
    val exception = result.exceptionOrNull()
    if (exception is GeminiClient.GeminiApiException) {
        val errorResponse = exception.errorResponse
        println("Error: ${errorResponse.error}")
        println("Message: ${errorResponse.message}")
        println("Status: ${errorResponse.statusCode}")
    }
}
```

## Testing

Run tests with:

```bash
./gradlew test
```

### Test Coverage

- `GeminiClientTest`: Tests API client functionality
- `GeminiRepositoryTest`: Tests repository layer
- `GeminiIntegrationTest`: Tests complete workflow

## Dependencies

- Kotlin 1.9.0
- Kotlinx Coroutines 1.7.3
- Kotlinx Serialization 1.6.0
- Retrofit 2.9.0
- OkHttp 4.11.0
- MockK 1.13.5 (for testing)

## Build

```bash
./gradlew build
```

## License

MIT License

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## Support

For issues and feature requests, please use the GitHub issue tracker.
# Android Project Scaffolding

This Android project implements a complete overlay UI and integration harness for GENOS (Generic Enhanced Navigation and Operation System) using Accessibility Service and WindowManager APIs.

## Overview

GENOS provides a persistent overlay that visualizes touch targets, gestures, and system status while enabling automated command execution across any Android application. The system includes:

- **Persistent Overlay UI** - Floating window showing GENOS status and controls
- **Command Pipeline** - Screen reading → command interpretation → input execution → status feedback
- **Touch Visualization** - Real-time gesture highlighting with animated feedback
- **OCR Integration** - ML Kit-powered text recognition with instant results
- **Accessibility Integration** - Complete automation of any Android app

## Architecture

### Core Components

- **GenosStatus** - System status model tracking automation state, current app, and Gemini decisions
- **GeminiActionPlan** - Action plan model containing sequence of ActionCommands
- **ActionCommand** - Individual command model (tap, swipe, type, wait, etc.)
- **CommandParameters** - Parameter container for action commands
- **OverlayService** - WindowManager-based floating HUD with drag controls
- **InputEmulatorService** - AccessibilityService for rootless input emulation
- **CommandExecutor** - Sequential command execution with timeout and error handling
- **MainActivity** - Debug UI for testing all components

### Features Implemented

#### 1. WindowManager Overlay
- ✅ Floating HUD with drag handle
- ✅ Real-time status display (GENOS state, current app, Gemini decision)
- ✅ Controls for automation toggle and test execution
- ✅ Permission handling for overlay display
- ✅ Auto-positioning and drag functionality

#### 2. Input Emulator Service
- ✅ AccessibilityService integration with `dispatchGesture`
- ✅ Tap gestures at specified coordinates
- ✅ Long press gestures
- ✅ Swipe/scroll gestures with custom paths
- ✅ Text input via `ACTION_SET_TEXT` and IME fallback
- ✅ System gestures (back, home, recents)
- ✅ Timeout handling and error reporting

#### 3. Command Executor
- ✅ Sequential execution of action plans
- ✅ Timeout safeguards (30-second default)
- ✅ User cancellation capability
- ✅ Error reporting with continue/stop logic
- ✅ Execution callbacks for UI updates
- ✅ Mock plan execution for testing

#### 4. Integration Features
- ✅ Service binding between InputEmulator and CommandExecutor
- ✅ Real-time overlay updates during execution
- ✅ Manual command injection via debug UI
- ✅ Custom command parsing (tap(x,y), swipe(x1,y1,x2,y2), type(text))

## Usage Instructions

### Setup

1. **Grant Overlay Permission**
   - Enable "Overlay Permission" in the app
   - Grant permission in system dialog

2. **Enable Accessibility Service**
   - Toggle "Accessibility Service" in the app
   - Enable "GENOS Input Emulator Service" in system settings

3. **Test Individual Commands**
   - Use "Test Tap", "Test Swipe", "Test Type" buttons
   - Try custom commands like:
     - `tap(200,400)` - Tap at screen coordinates
     - `swipe(100,500,300,300,400)` - Swipe from (100,500) to (300,300) over 400ms
     - `type(Hello World!)` - Type text into focused field

4. **Execute Mock Gemini Plan**
   - Click "Execute Mock Gemini Plan" to run a complete sequence
   - Observe real device actions (home, wait, swipe)

### Acceptance Criteria Validation

✅ **Overlay Toggle**: Overlay can be shown/hidden with toggle switch  
✅ **Manual Command Injection**: Debug UI supports real taps/swipes/types  
✅ **Real Actions**: Executing mocked Gemini response results in visible on-device actions  
✅ **Input Emulation**: Rootless gesture dispatch via AccessibilityService  
✅ **Command Execution**: Sequential plan execution with error handling  
✅ **Status Display**: Real-time GENOS status and app information  

## Technical Details

### Permissions Required
- `SYSTEM_ALERT_WINDOW` - For overlay display
- `BIND_ACCESSIBILITY_SERVICE` - For input emulation
- `INTERNET` - For future Gemini integration

### API Requirements
- Minimum SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- Uses `dispatchGesture()` API (API 24+)

### Key Technologies
- **WindowManager** - Floating overlay implementation
- **AccessibilityService** - Rootless input emulation
- **GestureDescription** - Modern gesture API
- **CommandExecutor Pattern** - Sequential action execution
- **Service Binding** - Inter-service communication

## Project Structure

```
com/genos/overlay/
├── GenosOverlayApplication.java     # Application class
├── ui/
│   └── MainActivity.java             # Debug UI and main entry point
├── service/
│   ├── OverlayService.java           # WindowManager overlay
│   ├── InputEmulatorService.java     # Accessibility input emulation
│   └── CommandExecutor.java          # Action plan execution
├── model/
│   ├── GenosStatus.java              # System status model
│   ├── GeminiActionPlan.java         # Action plan model
│   ├── ActionCommand.java            # Command model
│   └── CommandParameters.java        # Command parameters
└── receiver/
    └── BootReceiver.java             # Boot completion handler
```

## Future Enhancements

1. **Gemini Integration** - Real Gemini AI decision making
2. **Advanced UI Targeting** - Element-based interactions
3. **Recording/Replay** - User action recording system
4. **Configuration Management** - User preferences and settings
5. **Security Features** - Permission validation and safety checks
6. **Performance Optimization** - Gesture batching and optimization

## Building and Testing

The project uses standard Android build tools:

```bash
# Build APK
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- This implementation provides a complete foundation for the GENOS overlay system
- All core features are implemented and functional
- The system is ready for integration with actual Gemini AI
- Extensive logging and error handling ensures reliability
- The debug UI allows comprehensive testing of all features
The project contains the following packages in `app/src/main/java/com/example/androidproject/`:

- **`accessibility`**: AccessibilityService implementation for UI tree inspection and input injection  
- **`vision`**: Screen capture and ML Kit OCR integration
- **`command`**: Command parsing and execution system (tap, swipe, scroll, input, wait)
- **`overlay`**: WindowManager-based persistent overlay with gesture visualization
- **`ai`**: Placeholder for LLM integration (Gemini API ready)

### Integration Pipeline

```
Screen Content → Accessibility Service → UI Hierarchy
                    ↓
Command Input → Command Processor → Parsed Command
                    ↓
Command Executor → Gesture/Input → Accessibility Actions
                    ↓
Gesture Overlay ← Visual Feedback ← Status Updates
```

## Key Features

### 1. Persistent Overlay (`overlay/OverlayService.kt`)
- **Always-on-top** using `TYPE_APPLICATION_OVERLAY` across all apps
- **Real-time status** showing current app, automation state, and last action
- **Touch visualization** with animated ripples, swipe paths, and scroll indicators
- **Toggle controls** for start/stop automation, OCR requests, and UI tree display

### 2. Command Processing (`command/`)
- **Natural language commands**: `tap 500 500`, `swipe 100 1000 700 500`, `scroll down`
- **Batch execution**: Parse and execute multiple commands in sequence
- **Error handling**: Graceful handling of invalid commands and edge cases
- **Visual feedback**: Every command triggers overlay status and touch visualization updates

### 3. Touch Visualization (`overlay/GestureOverlayView.kt`)
- **Instant feedback**: Pink ripple effect for taps, green/red circles for swipe start/end
- **Gesture trails**: Animated paths showing swipe directions and distances
- **Scroll indicators**: Multiple parallel lines showing scroll direction
- **Automatic cleanup**: Visualizations fade after 2-3 seconds

### 4. OCR Integration (`vision/`, ML Kit)
- **On-demand text recognition** via overlay button
- **Real-time results** displayed directly in overlay UI
- **UI tree display** showing accessibility node hierarchy for screen reading

## 🏗️ Architecture

The application follows a modular architecture with four main components:

### Core Components

1. **ScreenCaptureManager** (`com.example.androidproject.vision.ScreenCaptureManager`)
   - Handles MediaProjection API integration
   - Periodic screenshot capture triggered by screen changes
   - Memory buffer management for captured Bitmaps
   - User consent flow via `startActivityForResult`
### Environment Variables
The project uses `buildConfig` fields for sensitive data. Currently configured in `app/build.gradle.kts`:

- `GEMINI_API_KEY`: Placeholder for Gemini AI integration
- `GEMINI_ENDPOINT`: Gemini API endpoint

### Required Permissions
- `SYSTEM_ALERT_WINDOW` - Overlay display over other apps
- `BIND_ACCESSIBILITY_SERVICE` - Accessibility automation and UI inspection  
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PROJECTION` - Background operation
- `INTERNET` - ML Kit and AI API access

## Usage

### 1. First Launch
1. Launch the app
2. Tap **"Enable Overlay & Accessibility Permissions"**
3. Grant overlay permission when prompted
4. Enable **"GENOS Accessibility Service"** in Settings → Accessibility
5. Return to app and tap **"Launch GENOS Overlay"**

### 2. Basic Commands
The overlay provides control buttons, or send commands via the app console:

```
tap 500 500          # Tap at coordinates (x, y)
swipe 100 1000 700 500 # Swipe from (x1, y1) to (x2, y2)
scroll down          # Scroll down on scrollable content
ocr                  # Trigger OCR on current screen
tree                 # Show/hide UI tree hierarchy
```

### 3. Integration Demo
**Complete automation pipeline:**
1. Navigate to target app
2. Tap **"Tree"** button to inspect UI hierarchy
3. Identify target coordinates from UI tree
4. Send tap command: `tap 300 800`
5. **Observe**: Visual feedback on overlay + action execution
6. Trigger OCR: `ocr`
7. **Observe**: Recognized text appears in overlay

### 4. Overlay Controls
- **Start/Stop**: Toggle automation monitoring state
- **OCR**: Perform text recognition on current screen
- **Tree**: Show/hide detailed UI tree for debugging

2. **OcrProcessor** (`com.example.androidproject.vision.OcrProcessor`)
   - ML Kit Text Recognition with bounding box extraction
   - Regional OCR processing for focused areas
   - Text block, line, and element hierarchy extraction
   - Coroutine-based async processing

3. **TesseractOcrProcessor** (`com.example.androidproject.vision.TesseractOcrProcessor`)
   - Alternative OCR implementation using Tesseract
   - Configurable recognition parameters
   - Regional processing capabilities
   - Asset-based language data management

4. **ScreenStateAggregator** (`com.example.androidproject.vision.ScreenStateAggregator`)
   - Screenshot persistence with URI management
   - OCR text and bounding box extraction
   - UI element processing and hierarchy
   - Aggregated payload creation for Gemini AI
   - Storage cleanup and management

5. **Accessibility Service** (`com.example.androidproject.accessibility.MyAccessibilityService`)
   - Real-time accessibility tree monitoring
   - UI element bounding boxes and properties
   - View hierarchy path tracking
   - Interactive element detection

## 🚀 Quick Start

### Basic Usage

```kotlin
// Initialize the coordinator
val coordinator = ScreenCaptureCoordinator(context, activity)

// Request permissions and start pipeline
val initialized = coordinator.initializeScreenCapture()
if (initialized) {
    coordinator.startPipeline()
}

// Trigger manual capture
val result = coordinator.triggerManualCapture()
when (result) {
    is ScreenStateResult.Success -> {
        val screenState = result.screenState
        // Process aggregated data
        processScreenState(screenState)
    }
    is ScreenStateResult.Error -> {
        Log.e("Capture", "Error: ${result.message}")
    }
}
```

### OCR Engine Selection

```kotlin
// Use ML Kit OCR (default)
val mlKitProcessor = OcrProcessor()
val mlKitResult = mlKitProcessor.processImage(bitmap)

// Use Tesseract OCR
val tesseractProcessor = TesseractOcrProcessor(context)
val initialized = tesseractProcessor.initialize()
if (initialized) {
    val tesseractResult = tesseractProcessor.processImage(bitmap)
}
```

## 📱 User Consent Flow

The application implements a comprehensive consent flow:

1. **Accessibility Permission**: Users enable the service via Settings
2. **Screen Capture Consent**: Users grant MediaProjection permission
3. **Lifecycle Management**: Automatic cleanup and stopping

## 🧪 Testing

The project includes comprehensive unit tests:

```bash
# Run unit tests
./gradlew test

# Run integration tests
./gradlew connectedAndroidTest
```

Test coverage includes:
- OCR processor initialization and processing
- Screen state aggregation
- Data structure validation
- Result type handling
- Performance comparison between OCR engines

## 📋 Permissions

The app requests the following permissions:

- `INTERNET` - For network operations
- `FOREGROUND_SERVICE` - For background service operation
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` - For MediaProjection API (Android 14+)
- `SYSTEM_ALERT_WINDOW` - For overlay functionality
- `WRITE_EXTERNAL_STORAGE` - For screenshot persistence
- `BIND_ACCESSIBILITY_SERVICE` - For UI tree extraction

## 🔧 Dependencies

Key dependencies include:

- **ML Kit Text Recognition**: Primary OCR processing
- **Tesseract (tess-two)**: Alternative OCR implementation
- **Kotlin Coroutines**: Async processing and flow management
- **Compose UI**: Modern Android UI framework
- **Accessibility Service**: UI tree extraction
- **MediaProjection API**: Screen capture functionality

## 📊 Data Structures

### ScreenState Result
```kotlin
data class ScreenState(
    val screenshotUri: Uri,                    // Persistent screenshot location
    val timestamp: Long,                       // Capture timestamp
    val ocrText: String,                       // Extracted text content
    val ocrBoundingBoxes: List<TextBoundingBox>, // Text with positions
    val uiElements: List<UiElement>,           // UI hierarchy data
    val metadata: ScreenMetadata              // Processing metadata
)
```

### OCR Result
```kotlin
sealed class OcrResult {
    data class Success(val textBlocks: List<TextBlock>) : OcrResult()
    data class Error(val message: String) : OcrResult()
}
```

## 🎯 Acceptance Criteria Fulfillment

✅ **ScreenCaptureManager with MediaProjection API**: Implemented with proper lifecycle management

✅ **Periodic screenshots triggered by screen changes**: Real-time capture with memory buffer management

✅ **ML Kit Text Recognition integration**: Complete OCR processing with bounding boxes

✅ **Tesseract alternative OCR**: Full implementation with regional processing

✅ **Accessibility tree integration**: Real-time UI element hierarchy extraction

✅ **ScreenStateAggregator**: Comprehensive data packaging for Gemini AI

✅ **User consent flow**: startActivityForResult implementation

✅ **Lifecycle management**: Automatic stopping on screen off

✅ **Bitmap decoding**: Proper ImageReader handling and bitmap processing

✅ **Text snippets with bounding boxes**: Complete OCR result extraction

✅ **Aggregated payload for transmission**: Ready-to-send data structure

## 🔄 Example Usage Scenarios

See `ScreenCaptureExampleUsage.kt` for comprehensive examples including:

- Basic ML Kit OCR pipeline
- Tesseract OCR pipeline
- Regional OCR processing
- Complete pipeline with accessibility
- Performance comparison between OCR engines

## 📚 Documentation

- [Screen Capture Implementation](SCREEN_CAPTURE_README.md) - Detailed implementation guide
- [API Documentation](docs/) - Comprehensive API reference
- [Testing Guide](docs/testing.md) - Testing strategies and examples

## 🤝 Contributing

1. Follow the existing code style and patterns
2. Add unit tests for new functionality
3. Update documentation for API changes
4. Ensure all acceptance criteria are met

## 📄 License

This project is provided as-is for educational and development purposes.

---

**Note**: This implementation provides a complete foundation for screen capture and OCR processing. For production use, consider adding additional features like:
- Real-time compression optimization
- Selective region capture for performance
- Enhanced error handling and recovery
- Integration with Gemini API for intelligent processing
- Advanced filtering and noise reduction
### Prerequisites
- JDK 17 or higher
- Android Studio Hedgehog or newer
- Android device/emulator with API 26+ (Android 8.0)

### Build Commands
```bash
# Build APK
./gradlew build

# Install debug version
./gradlew installDebug

# Run instrumentation tests
./gradlew connectedAndroidTest
```

## Testing

### Automated Tests
- **Integration Tests**: `OverlayIntegrationTest.kt` validates complete pipeline
- **Command Parsing**: Tests all command variants and error handling
- **Touch Visualization**: Verifies gesture feedback system

```bash
# Run integration tests
./gradlew connectedAndroidTest --tests "*OverlayIntegrationTest"
```

### Manual Testing
See [TESTING_GUIDE.md](TESTING_GUIDE.md) for comprehensive manual test steps including:
- Overlay persistence across apps
- Touch visualization accuracy
- OCR functionality
- Command execution pipeline
- Edge cases and error handling

## Architecture Details

### Overlay Service Lifecycle
```
MainActivity → Request Permissions → Launch OverlayService
    ↓
WindowManager.addView() → TYPE_APPLICATION_OVERLAY
    ↓
ComposeView → OverlayView → GestureOverlayView
    ↓
BroadcastReceiver ← Commands ← AccessibilityService
```

### Command Execution Flow
```
Broadcast Command → OverlayService.onStartCommand()
    ↓
OverlayViewModel.updateStatus() → StateFlow → Compose Recomposition
    ↓
CommandProcessor.parseCommand() → Command.Tap/Swipe/Scroll
    ↓
CommandExecutor.executeCommand() → AccessibilityService.dispatchGesture()
    ↓
GestureOverlayView.showTouch()/showSwipe() → Visual Feedback
```

## Troubleshooting

### Overlay Not Visible
- Verify `SYSTEM_ALERT_WINDOW` permission granted in Settings → Apps → Special access
- Check logcat for `OverlayService` initialization errors

### Commands Not Executing
- Ensure Accessibility Service is enabled in Settings → Accessibility
- Check if target app is blocking accessibility events

### OCR Not Working
- Update Google Play Services for ML Kit compatibility
- Verify internet connectivity for ML Kit initialization

## Future Enhancements

- **AI Integration**: Connect Gemini API for intelligent command generation
- **Gesture Recording**: Record and replay complex gesture sequences
- **Multi-device Sync**: Coordinate automation across multiple devices
- **Plugin System**: Extensible command types and visualizations

## License

This project provides a complete overlay and automation framework for Android applications using modern Android development practices with Kotlin, Jetpack Compose, and Accessibility APIs.
