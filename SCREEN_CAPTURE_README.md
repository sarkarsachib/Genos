# Screen Capture and OCR Pipeline

This Android application implements a comprehensive screen capture and OCR pipeline that leverages the MediaProjection API for screen capture, ML Kit for OCR processing, and accessibility tree integration for UI element detection.

## Architecture Overview

The pipeline consists of four main components:

### 1. ScreenCaptureManager
- **Location**: `com.example.androidproject.vision.ScreenCaptureManager`
- **Purpose**: Handles MediaProjection API integration for periodic screen capture
- **Key Features**:
  - User consent flow via `startActivityForResult`
  - Automatic screenshot triggering when screen changes
  - Memory buffer management for captured Bitmaps
  - Thread-safe capture processing

### 2. OcrProcessor
- **Location**: `com.example.androidproject.vision.OcrProcessor`
- **Purpose**: Processes captured screens for text recognition using ML Kit
- **Key Features**:
  - ML Kit Text Recognition with bounding box extraction
  - Coroutine-based async processing
  - Regional OCR processing for focused areas
  - Text block, line, and element hierarchy extraction

### 3. AccessibilityTree Integration
- **Location**: `com.example.androidproject.accessibility.MyAccessibilityService`
- **Purpose**: Captures UI element hierarchy and properties
- **Key Features**:
  - Real-time accessibility tree monitoring
  - UI element bounding boxes and properties
  - View hierarchy path tracking
  - Interactive element detection

### 4. ScreenStateAggregator
- **Location**: `com.example.androidproject.vision.ScreenStateAggregator`
- **Purpose**: Combines screenshot, OCR results, and accessibility data
- **Key Features**:
  - Screenshot persistence with URI management
  - OCR text and bounding box extraction
  - UI element processing and hierarchy
  - Aggregated payload creation for Gemini AI
  - Storage cleanup and management

## User Consent Flow

1. **Accessibility Permission**: Users must enable the accessibility service via Settings
2. **Screen Capture Consent**: Users grant MediaProjection permission through system dialog
3. **Lifecycle Management**: Automatic stopping when screen is off or app is backgrounded

## Usage

### Basic Setup
```kotlin
// Initialize the coordinator
val coordinator = ScreenCaptureCoordinator(context, activity)

// Request permissions and start
val initialized = coordinator.initializeScreenCapture()
if (initialized) {
    coordinator.startPipeline()
}
```

### Manual Capture
```kotlin
// Trigger one-time capture
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

### Result Structure
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

## Dependencies

- **ML Kit Text Recognition**: OCR processing with bounding boxes
- **Kotlin Coroutines**: Async processing and flow management
- **Accessibility Service**: UI tree extraction
- **MediaProjection API**: Screen capture functionality
- **Compose UI**: Modern Android UI framework

## Testing

The implementation includes comprehensive unit tests in:
`app/src/test/java/com/example/androidproject/vision/ScreenCaptureAndOcrIntegrationTest.kt`

Tests cover:
- OCR processor initialization and processing
- Screen state aggregation
- Data structure validation
- Result type handling

## Permissions Required

1. **FOREGROUND_SERVICE**: For background screen capture
2. **FOREGROUND_SERVICE_MEDIA_PROJECTION**: For MediaProjection API (Android 14+)
3. **Accessibility Service**: For UI tree extraction
4. **WRITE_EXTERNAL_STORAGE**: For screenshot persistence

## Acceptance Criteria Fulfillment

✅ **Triggering capture yields decoded Bitmap**: Implemented in `ScreenCaptureManager` with proper ImageReader handling

✅ **OCR returns text snippets for on-screen content**: Implemented in `OcrProcessor` with ML Kit integration and bounding box extraction

✅ **Aggregated payload is ready for transmission**: Implemented in `ScreenStateAggregator` with comprehensive data packaging

✅ **User consent flow**: Implemented with startActivityForResult and settings navigation

✅ **Lifecycle management**: Implemented with automatic stopping on screen off and activity lifecycle handling

## Future Enhancements

- Tesseract OCR integration as ML Kit alternative
- Real-time compression optimization
- Selective region capture for performance
- Integration with Gemini API for intelligent processing
- Advanced filtering and noise reduction