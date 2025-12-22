# GENOS Overlay System
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

## Configuration

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

## Build Instructions

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