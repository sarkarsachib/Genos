# Screen Capture and OCR Android Application

This Android application implements a comprehensive screen capture and OCR pipeline that leverages the MediaProjection API for screen capture, ML Kit for OCR processing, accessibility tree integration for UI element detection, and is ready for Gemini AI integration.

## 🎯 Key Features

- **MediaProjection API Integration**: Real-time screen capture with user consent flow
- **Dual OCR Support**: ML Kit Text Recognition and Tesseract via tess-two
- **Accessibility Tree Integration**: Real-time UI element hierarchy and properties extraction
- **Screen State Aggregation**: Combined payload of screenshot, OCR text, and UI elements
- **Lifecycle Management**: Automatic stopping when screen is off or app is backgrounded
- **Comprehensive Testing**: Unit tests for all core components

## 🏗️ Architecture

The application follows a modular architecture with four main components:

### Core Components

1. **ScreenCaptureManager** (`com.example.androidproject.vision.ScreenCaptureManager`)
   - Handles MediaProjection API integration
   - Periodic screenshot capture triggered by screen changes
   - Memory buffer management for captured Bitmaps
   - User consent flow via `startActivityForResult`

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