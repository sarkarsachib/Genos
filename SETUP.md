# GENOS Phase 1 Setup Guide

## Prerequisites

- Android Studio Arctic Fox (2023.1.1) or later
- Android SDK 34
- JDK 17
- Google Gemini API Key (for production use)

## Build Instructions

### 1. Clone Repository

```bash
git clone <repository-url>
cd GenosCore
```

### 2. Configure API Key (Optional)

For production use with Gemini AI:

```bash
# Create gradle.properties in project root (if not exists)
echo "GEMINI_API_KEY=your_api_key_here" >> gradle.properties
```

**Note**: Without API key, the app will use mock/demo action plans.

### 3. Build the Project

Using Gradle wrapper:

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

Using Android Studio:
- Open project in Android Studio
- Wait for Gradle sync to complete
- Build -> Build Bundle(s) / APK(s) -> Build APK(s)

### 4. Install on Device

```bash
# Via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or use generated install command
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Required Permissions

The app requires the following permissions:

### 1. Accessibility Service
- **Purpose**: Enable automated tap, swipe, and text input
- **Grant path**: Settings > Accessibility > GENOS Accessibility Service
- **Required**: Yes

### 2. Overlay Permission
- **Purpose**: Display floating HUD overlay
- **Grant path**: Settings > Apps > Special Access > Display over other apps > GENOS
- **Required**: Yes

### 3. Screen Capture
- **Purpose**: Capture screen content for vision analysis
- **Grant path**: Requested when starting screen capture
- **Required**: Yes

### 4. Storage
- **Purpose**: Save screenshots and logs
- **Required**: Yes (Android 13+ uses MediaStore APIs)

### Optional Permissions

- **Camera**: For image capture (optional)
- **Microphone**: For audio analysis (optional)
- **Location**: For context-aware automation (optional)
- **Body Sensors**: For gesture detection (optional)

## Testing

### Unit Tests

```bash
./gradlew test
```

### Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

### Manual Testing

1. **Accessibility Service**:
   - Open app
   - Verify accessibility service status
   - Navigate to Accessibility Settings
   - Enable GENOS service
   - Return to app and verify enabled status

2. **Overlay**:
   - Tap "Show Overlay" button
   - Verify overlay appears on screen
   - Tap "Hide Overlay" button
   - Verify overlay disappears

3. **Action Execution**:
   - Tap "Execute Demo" button
   - Verify demo actions execute
   - Check overlay status updates

## Troubleshooting

### Build Issues

**Error**: "SDK location not found"
**Solution**: File > Project Structure > SDK Location > Set Android SDK path

**Error**: "Could not resolve com.google.mlkit:vision-common"
**Solution**: Add Google Play Services dependency
```gradle
implementation "com.google.android.gms:play-services-mlkit-vision:16.3.0"
```

**Error**: "Duplicate class in dependencies"
**Solution**: Check for version conflicts in build.gradle.kts

### Runtime Issues

**Accessibility service not enabled**:
- Check if service is in accessibility settings
- Verify it's enabled (toggle switch)
- Check logcat for errors

**Overlay not showing**:
- Verify overlay permission granted
- Check if "Draw over other apps" permission enabled
- Check logcat for WindowManager errors

**Screen capture fails**:
- Verify MediaProjection permission granted
- Check if screen capture service is running as foreground service
- Check logcat for security exceptions

**Actions not executing**:
- Verify accessibility service is enabled
- Check if service has necessary flags (canPerformGestures)
- Check logcat for dispatchGesture errors

### Debug Logging

Enable detailed logging by setting:

```kotlin
// In build.gradle.kts
buildConfigField("boolean", "DEBUG_MODE", "true")
```

View logs:
```bash
adb logcat | grep "GENOS"
adb logcat | grep "Genos"
```

## Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for detailed architecture documentation.

## Module Structure

```
ai/genos/core/
├── accessibility/      # AccessibilityService, gesture emulation
├── vision/            # Screen capture, OCR, ML Kit
├── audio/             # Speech recognition (placeholder)
├── sensors/           # Device sensors monitoring
├── ai/                # Gemini integration, action models
│   ├── gemini/       # Gemini API client
│   └── models/        # ActionPlan, GenosAction
├── execution/          # Command execution service
├── privilege/          # Shizuku integration (placeholder)
├── state/             # State management (placeholder)
└── ui/                # Activities, overlay service
```

## Next Steps

After setup:

1. Test all core functionality
2. Configure Gemini API key for production
3. Implement custom action plans
4. Add vision analysis (OCR, object detection)
5. Integrate with your specific automation use case

## Support

For issues or questions:
- Check existing documentation in /docs folder
- Review logcat output
- Verify all permissions are granted
- Test with demo action plan first
