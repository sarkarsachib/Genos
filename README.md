# GENOS Phase 1 - Android Initialization

A modular Android project implementing a WindowManager overlay with input emulation and command execution capabilities using accessibility services and Gemini AI integration.

## Quick Start

### Prerequisites
- Android Studio Arctic Fox (2023.1.1) or later
- Android SDK 34
- JDK 17
- Google Gemini API Key (optional - for production AI features)

### Build and Run

```bash
# Build the project
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

See [SETUP.md](SETUP.md) for detailed build and installation instructions.

## Project Structure

```
app/src/main/
├── kotlin/ai/genos/core/
│   ├── accessibility/      # AccessibilityService for input emulation
│   ├── vision/            # Screen capture via MediaProjection
│   ├── audio/             # Speech recognition (placeholder)
│   ├── sensors/           # Device sensor monitoring
│   ├── ai/                # Gemini AI integration
│   │   ├── gemini/       # API client
│   │   └── models/        # ActionPlan, GenosAction
│   ├── execution/          # Command execution service
│   ├── privilege/          # Shizuku integration (optional)
│   ├── state/             # State management (placeholder)
│   └── ui/                # Activities and overlay service
└── res/                  # Android resources
    ├── layout/            # XML layouts
    ├── values/            # Strings, colors, themes
    └── xml/               # Permissions, accessibility config
```

## Core Features

### 1. Accessibility Service
- Rootless input emulation using `dispatchGesture()`
- UI element discovery via AccessibilityNodeInfo
- Tap, swipe, text input, scroll execution
- Screen content extraction

### 2. Screen Capture
- MediaProjection API integration
- Foreground service for screen capture
- Bitmap conversion for vision analysis
- Configurable capture settings

### 3. AI Integration
- Gemini API client using Retrofit
- Action plan generation from screen content
- Support for multiple action types:
  - TAP, SWIPE, TEXT, SCROLL, LAUNCH, WAIT
- Mock/demo action plan support

### 4. Command Execution
- Sequential action execution
- Foreground service for reliability
- Real-time status updates
- Error handling and recovery

### 5. UI Overlay
- WindowManager floating HUD
- Real-time status display
- Last action feedback
- Toggle functionality

## Permissions

### Required
- `BIND_ACCESSIBILITY_SERVICE` - Input emulation
- `SYSTEM_ALERT_WINDOW` - Overlay display
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` - Screen capture
- `INTERNET` - AI API communication

### Optional
- `CAMERA` - Image capture
- `RECORD_AUDIO` - Speech recognition
- `BODY_SENSORS` - Gesture detection
- `ACCESS_FINE_LOCATION` - Context-aware automation

See [SETUP.md](SETUP.md) for complete permission guide.

## Configuration

### BuildConfig Fields

```kotlin
// Access in code:
BuildConfig.GEMINI_API_KEY       // Your API key
BuildConfig.GEMINI_ENDPOINT      // "https://generativelanguage.googleapis.com"
BuildConfig.GEMINI_MODEL        // "gemini-2.0-flash"
BuildConfig.DEBUG_MODE           // Boolean for logging
```

### Setting API Key

Create `gradle.properties` in project root:

```properties
GEMINI_API_KEY=your_api_key_here
```

Without API key, the app uses demo/mock action plans.

## Documentation

- **[SETUP.md](SETUP.md)** - Complete build and setup guide
- **[architecture/STRUCTURE.md](architecture/STRUCTURE.md)** - Package structure and architecture

## Key Components

### GenosAccessibilityService
Main accessibility service providing input emulation capabilities.

```kotlin
val service = GenosAccessibilityService.getInstance()
service?.performTap(500f, 1000f, callback)
service?.performSwipe(0f, 0f, 500f, 1000f, 300L, callback)
```

### ScreenCaptureService
Foreground service for screen capture.

```kotlin
val service = ScreenCaptureService.getInstance()
service?.startCapture(resultCode, dataIntent)
val bitmap = service?.captureScreenshot()
```

### GeminiClient
AI client for action plan generation.

```kotlin
val client = GeminiClient.getInstance(context)
val result = client.generateActionPlan(screenContent, userInstruction)
```

### CommandExecutorService
Executes action plans.

```kotlin
val intent = Intent(context, CommandExecutorService::class.java).apply {
    action = CommandExecutorService.ACTION_EXECUTE_PLAN
    putExtra(CommandExecutorService.EXTRA_ACTION_PLAN, actionPlan)
}
startService(intent)
```

## Dependencies

### Core
- AndroidX Core (AppCompat, Material, ConstraintLayout)
- Kotlin Coroutines 1.7.3
- Jetpack (Lifecycle, Navigation, WorkManager, Room)
- Jetpack Compose (optional)

### AI & Vision
- Google ML Kit (Text, Face, Pose, Barcode)
- Tesseract OCR (tess-two 9.1.0)
- Retrofit 2.9 + OkHttp 4.12
- Gson for JSON parsing

### Utilities
- Timber 5.0.1 (Logging)
- Accompanist (System UI, Permissions)
- Shizuku 13.1.5 (Optional privilege escalation)

## Build Configuration

- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 34 (Android 14)
- **compileSdk**: 34
- **Kotlin**: 1.9.20
- **Gradle**: 8.2.0
- **Build Tools**: 34.0.0

## Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Manual Testing Workflow
1. Install app on device
2. Grant Accessibility Service permission
3. Grant Overlay permission
4. Start Screen Capture service
5. Execute demo action plan
6. Verify overlay status updates

## Troubleshooting

### Build Errors
- **"Duplicate class"**: Check dependency versions in build.gradle.kts
- **"SDK not found"**: Configure SDK path in Android Studio
- **"R8 full mode"**: Increase heap size in gradle.properties

### Runtime Errors
- **"Accessibility service not enabled"**: Enable in Settings > Accessibility
- **"Overlay not showing"**: Grant "Display over other apps" permission
- **"Screen capture failed"**: Start foreground service before capture
- **"Actions not executing"**: Verify service has canPerformGestures flag

## Development

### Branching
Current branch: `feat-genos-phase1-android-init`

### Code Style
- Kotlin-first with coroutines
- Timber for logging
- Result types for error handling
- Clear separation of concerns

## License

This project is part of the GENOS initiative for AI-powered device automation.

## Support

For issues:
1. Check [SETUP.md](SETUP.md) troubleshooting section
2. Review architecture in [architecture/STRUCTURE.md](architecture/STRUCTURE.md)
3. Check logcat: `adb logcat | grep GENOS`
