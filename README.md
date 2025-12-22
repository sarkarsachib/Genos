# Android Project Scaffolding

This is a modern Android project scaffold initialized with Kotlin, Android 14 target, and modular package structure.

## Structure

The project contains the following packages in `app/src/main/java/com/example/androidproject/`:
- `accessibility`: Contains Accessibility Service implementations.
- `vision`: Contains Screen Capture and Vision-related services.
- `ai`: Placeholder for AI/LLM integration.
- `command`: Placeholder for command processing.

## Configuration

### Environment Variables
The project uses `buildConfig` fields for sensitive data. 
Currently, the following fields are defined in `app/build.gradle.kts`:

- `GEMINI_API_KEY`: Placeholder key for Gemini API.
- `GEMINI_ENDPOINT`: Endpoint for Gemini API.

To update these, modify the `buildConfigField` values in `app/build.gradle.kts` or use local properties if you set up a secrets management plugin.

## Build Instructions

1.  **Prerequisites**:
    - JDK 17 or higher.
    - Android Studio Hedgehog or newer (or command line tools).

2.  **Build**:
    Run the following command in the root directory:
    ```bash
    ./gradlew build
    ```

    To install the debug APK:
    ```bash
    ./gradlew installDebug
    ```

## Permissions

The app requests the following permissions:
- `INTERNET`
- `SYSTEM_ALERT_WINDOW` (Overlay)
- `BIND_ACCESSIBILITY_SERVICE`
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PROJECTION`

Legacy external storage access is requested for older devices.
