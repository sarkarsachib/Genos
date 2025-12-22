# GENOS Overlay System

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