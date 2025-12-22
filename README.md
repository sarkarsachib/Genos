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
