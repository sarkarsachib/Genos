# GENOS Overlay System

A comprehensive Android application implementing a WindowManager-based overlay with input emulation and command execution capabilities.

## Overview

This project implements a sophisticated Android system that provides:

1. **WindowManager Overlay (HUD)** - A persistent floating overlay showing GENOS status, current app, and Gemini decision summary
2. **Input Emulator Service** - Rootless input emulation using AccessibilityService with dispatchGesture for taps, swipes, typing
3. **Command Executor** - Sequential execution of Gemini action plans with error reporting and safeguards

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