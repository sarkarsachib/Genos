# Genos Core - Installation & Setup Guide

## Prerequisites

### Development Environment
- **Android Studio**: Arctic Fox or later (recommended: latest stable)
- **JDK**: 17 or later
- **Android SDK**: API 24+ installed
- **Gradle**: 8.2+ (included via wrapper)

### For Testing
- **Android Device or Emulator**: API 24+ (Android 7.0+)
- **ADB**: Android Debug Bridge (included with Android SDK)
- **USB Debugging**: Enabled on physical device

## Setup Instructions

### 1. Clone the Repository
```bash
git clone <repository-url>
cd genos-core
```

### 2. Configure Android SDK
Create `local.properties` in the project root:
```properties
sdk.dir=/path/to/your/Android/Sdk
```

Or let Android Studio auto-generate it when you open the project.

### 3. Sync and Build
```bash
./gradlew build
```

## Building the Application

### Debug Build
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

### Clean Build
```bash
./gradlew clean assembleDebug
```

## Installing the Application

### Install Debug APK via Gradle
```bash
./gradlew installDebug
```

### Install via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Install via Android Studio
1. Open project in Android Studio
2. Connect device or start emulator
3. Click Run (green play button) or press Shift+F10

## Required Permissions Setup

After installation, the app requires manual permission setup:

### 1. Enable Accessibility Service
1. Open device **Settings** → **Accessibility**
2. Find **Genos Accessibility Service**
3. Toggle ON and confirm

Or use the app's **"Enable Accessibility Service"** button to open settings.

### 2. Grant Overlay Permission
1. Open device **Settings** → **Apps** → **Genos Core** → **Display over other apps**
2. Toggle ON **Allow display over other apps**

Or use the app's **"Enable Overlay Permission"** button.

### 3. Grant Notification Permission (Android 13+)
The app will request notification permission at runtime for foreground services.

## Testing the Installation

### 1. Launch the App
Open **Genos Core** from app drawer.

### 2. Verify Main Screen
You should see:
- "Genos Core" title
- "AI Assistant Framework" subtitle
- Three buttons:
  - Enable Accessibility Service
  - Enable Overlay Permission
  - Open Settings

### 3. Check Settings Activity
Tap **"Open Settings"** to verify the settings screen loads.

### 4. Verify Accessibility Service
```bash
adb shell settings get secure enabled_accessibility_services
```
Should show `ai.genos.core/.service.GenosAccessibilityService` if enabled.

### 5. Check Logcat for Service Logs
```bash
adb logcat -s GenosAccessibilityService ScreenCaptureService OverlayService InputProcessingService
```

## Troubleshooting

### Build Failures

#### SDK Location Not Found
**Error**: `SDK location not found`

**Solution**: Create `local.properties` with correct SDK path.

#### Dependency Resolution Failure
**Error**: `Could not resolve dependency`

**Solution**: 
```bash
./gradlew --refresh-dependencies assembleDebug
```

#### Out of Memory
**Error**: `java.lang.OutOfMemoryError`

**Solution**: Edit `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m
```

### Installation Issues

#### Installation Failed
**Error**: `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

**Solution**: Uninstall existing app:
```bash
adb uninstall ai.genos.core
```

#### Permission Denied
**Error**: `adb: insufficient permissions for device`

**Solution**: 
```bash
adb kill-server
adb start-server
```

### Runtime Issues

#### Service Not Starting
**Problem**: Accessibility service doesn't work

**Check**:
1. Service enabled in Settings → Accessibility
2. Check logcat for errors
3. Verify manifest declares service correctly

#### Overlay Not Showing
**Problem**: Overlay windows don't appear

**Check**:
1. Overlay permission granted
2. Check Settings → Apps → Display over other apps
3. For some devices: disable battery optimization

## Development Workflow

### 1. Make Changes
Edit code in `app/src/main/java/ai/genos/core/`

### 2. Build and Install
```bash
./gradlew installDebug
```

### 3. View Logs
```bash
adb logcat | grep -E "Genos|ai.genos.core"
```

### 4. Debug
- Use Android Studio debugger
- Set breakpoints in code
- Run in debug mode (Shift+F9)

## Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

### Lint Check
```bash
./gradlew lint
```

## APK Details

### Debug APK
- **Name**: `app-debug.apk`
- **Size**: ~76MB (includes native libs for Tesseract)
- **Signed**: Debug keystore
- **Debuggable**: Yes
- **Minification**: Disabled

### Release APK (Future)
- **Name**: `app-release.apk`
- **Signed**: Production keystore (required)
- **Debuggable**: No
- **Minification**: Enabled with ProGuard/R8

## Next Steps

After successful installation:

1. **Grant all permissions** (accessibility, overlay, notifications)
2. **Test service activation** (enable accessibility service)
3. **Verify logging** (check logcat for service lifecycle events)
4. **Review documentation**:
   - `README.md` - Project overview
   - `ARCHITECTURE.md` - Architecture details
   - `BUILD_VERIFICATION.md` - Build verification report

## Support & Development

### Check Build Status
```bash
./gradlew tasks
```

### List Available Tasks
```bash
./gradlew tasks --all
```

### Project Info
```bash
./gradlew projects
```

### Dependency Tree
```bash
./gradlew app:dependencies
```

## Important Notes

⚠️ **Security**: This app requires sensitive permissions (accessibility, overlay). Use responsibly.

⚠️ **Battery**: Accessibility services can impact battery life. Optimize service usage.

⚠️ **Privacy**: The app can access screen content. Be transparent with users.

⚠️ **Testing**: Test on multiple devices and Android versions (API 24-34).

## Useful ADB Commands

```bash
# List connected devices
adb devices

# Install APK
adb install -r app-debug.apk

# Uninstall app
adb uninstall ai.genos.core

# Clear app data
adb shell pm clear ai.genos.core

# Force stop app
adb shell am force-stop ai.genos.core

# Launch main activity
adb shell am start -n ai.genos.core/.MainActivity

# Check app info
adb shell dumpsys package ai.genos.core

# View app logs only
adb logcat -s ai.genos.core

# Check accessibility services
adb shell settings get secure enabled_accessibility_services

# Grant permission (root/adb only)
adb shell pm grant ai.genos.core android.permission.SYSTEM_ALERT_WINDOW
```

## Conclusion

The Genos Core app is now installed and ready for development and testing. All Phase 1 infrastructure is in place, and the app is ready for Phase 2 implementation of core functionality.
