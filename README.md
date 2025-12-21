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
