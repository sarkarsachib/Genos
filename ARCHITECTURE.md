# Genos Core Architecture

## Overview

Genos Core is built as a modular Android application designed to provide AI assistant capabilities through screen monitoring, text recognition, and intelligent input processing.

## Phase 1: Foundation Setup

### Objectives
1. ✅ Establish Android project structure
2. ✅ Configure build system with all required dependencies
3. ✅ Set up manifest with permissions and service declarations
4. ✅ Create base package structure with placeholder implementations
5. ✅ Ensure project builds and installs successfully

### Architecture Layers

```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│  (MainActivity, SettingsActivity, UI)   │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│           Service Layer                 │
│  (AccessibilityService, Capture,        │
│   Overlay, Input Processing)            │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│           Core Logic Layer              │
│  (OverlayManager, InputProcessor,       │
│   ScreenCaptureManager, TextRecognizer) │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│           Data Layer                    │
│  (Future: Room DB, Preferences)         │
└─────────────────────────────────────────┘
```

## Core Components

### 1. Accessibility Service (`service/GenosAccessibilityService`)
**Purpose**: Monitor and interact with screen content

**Capabilities**:
- Receive accessibility events from system and apps
- Read screen content without screenshot
- Respond to UI changes in real-time
- Access window content and layout hierarchy

**Configuration**: `res/xml/accessibility_service_config.xml`

**Permissions**: `BIND_ACCESSIBILITY_SERVICE`

### 2. Screen Capture Service (`service/ScreenCaptureService`)
**Purpose**: Capture screenshots for OCR processing

**Capabilities**:
- Use MediaProjection API for screen capture
- Run as foreground service with notification
- Provide bitmap data to OCR engines

**Permissions**: 
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MEDIA_PROJECTION`
- User consent via MediaProjection API

### 3. Overlay Service (`service/OverlayService`)
**Purpose**: Display floating UI elements

**Capabilities**:
- Show/hide overlay windows
- Display AI responses and interactions
- Handle touch events on overlays

**Permissions**: `SYSTEM_ALERT_WINDOW`

### 4. Input Processing Service (`service/InputProcessingService`)
**Purpose**: Process and route user input

**Capabilities**:
- Handle text input
- Process voice input (future)
- Route input to AI backend (future)

## Module Descriptions

### Overlay Module (`overlay/`)
Manages floating windows and overlay UI elements.

**Key Classes**:
- `OverlayManager`: Creates and manages overlay windows

**Future Enhancements**:
- Draggable overlay windows
- Customizable overlay appearance
- Multiple overlay types (popup, persistent, notification-style)

### Input Module (`input/`)
Processes user input from various sources.

**Key Classes**:
- `InputProcessor`: Handles text and voice input

**Future Enhancements**:
- Voice recognition integration
- Natural language processing
- Context-aware input handling
- Multi-modal input (text, voice, gestures)

### Capture Module (`capture/`)
Handles screen capture and text extraction.

**Key Classes**:
- `ScreenCaptureManager`: Manages MediaProjection lifecycle
- `TextRecognizer`: Provides OCR capabilities

**OCR Strategies**:
1. **ML Kit Text Recognition**: Fast, on-device, good for general text
2. **Tesseract**: More accurate, supports multiple languages, slower

**Future Enhancements**:
- Selective region capture
- Video/continuous capture
- Image preprocessing for better OCR
- OCR result caching and optimization

## Data Flow

### Screen Content → Text Recognition Flow
```
User Action
    ↓
Accessibility Event / Screen Capture
    ↓
ScreenCaptureManager captures bitmap
    ↓
TextRecognizer processes with ML Kit/Tesseract
    ↓
Recognized text → InputProcessor
    ↓
Process with AI (future)
    ↓
Display result via OverlayService
```

### Input Processing Flow
```
User Input (Text/Voice)
    ↓
InputProcessor receives input
    ↓
Context enrichment (screen content, history)
    ↓
AI Processing (future)
    ↓
Response generation
    ↓
Display via Overlay or MainActivity
```

## Technology Stack

### Build System
- **Gradle**: 8.2.0 with Kotlin DSL
- **AGP**: 8.2.0 (Android Gradle Plugin)
- **Kotlin**: 1.9.20

### UI Framework
- **Jetpack Compose**: Modern declarative UI
- **Material 3**: Design system
- **ViewBinding**: For legacy views if needed

### Android Jetpack
- **Lifecycle**: Lifecycle-aware components
- **ViewModel**: UI state management
- **LiveData**: Reactive data observation
- **Navigation**: Fragment/Compose navigation
- **Room**: Local database (future)
- **WorkManager**: Background tasks (future)

### ML/AI
- **ML Kit Text Recognition v2**: On-device OCR
- **Tesseract 4**: Advanced OCR engine
- **TensorFlow Lite**: Custom models (future)

### Concurrency
- **Kotlin Coroutines**: Async operations
- **Flow**: Reactive streams (future)

## Security Considerations

### Permissions
- All permissions are declared in manifest
- Runtime permission requests in UI
- Special permissions require user navigation to settings

### Data Protection
- Sensitive data should be encrypted
- Screenshots should be processed and discarded
- User consent for data collection

### Privacy
- Clear disclosure of what is monitored
- User control over service activation
- No data sent to external servers without consent

## Performance Considerations

### Memory Management
- Bitmap recycling after OCR processing
- Service lifecycle management
- Avoid memory leaks in services

### Battery Optimization
- Use foreground services only when necessary
- Batch processing when possible
- Respect system battery optimization

### Processing Optimization
- Choose appropriate OCR engine based on use case
- Cache OCR results when appropriate
- Use WorkManager for non-urgent tasks

## Future Phases

### Phase 2: Core Functionality
- Implement full accessibility service
- Complete MediaProjection integration
- Working ML Kit and Tesseract OCR
- Basic overlay functionality

### Phase 3: AI Integration
- Integrate AI/LLM backend
- Context-aware responses
- Learning and personalization

### Phase 4: Advanced Features
- Voice interaction
- Multi-app workflows
- Automation capabilities
- Plugin system

## Testing Strategy

### Unit Tests
- Logic in InputProcessor
- Text recognition algorithms
- Utility functions

### Integration Tests
- Service lifecycle
- Permission flows
- OCR accuracy

### UI Tests
- Compose UI interactions
- Permission request flows
- Settings configuration

### Manual Testing
- Accessibility service on various apps
- Screen capture across different devices
- Overlay display and interaction

## Development Workflow

1. **Feature Development**
   - Create feature branch
   - Implement with tests
   - Code review
   - Merge to main

2. **Testing**
   - Run unit tests: `./gradlew test`
   - Run instrumentation tests: `./gradlew connectedAndroidTest`
   - Manual testing on physical devices

3. **Build & Release**
   - Build debug: `./gradlew assembleDebug`
   - Build release: `./gradlew assembleRelease`
   - Sign and distribute

## Debugging

### Logcat Tags
- `GenosAccessibilityService`: Accessibility events
- `ScreenCaptureService`: Screen capture operations
- `OverlayManager`: Overlay lifecycle
- `InputProcessor`: Input processing
- `TextRecognizer`: OCR operations

### Common Issues
- **Service not starting**: Check manifest and permissions
- **Overlay not showing**: Verify SYSTEM_ALERT_WINDOW permission
- **OCR not working**: Check ML Kit model download and Tesseract data files
- **Accessibility events not received**: Ensure service is enabled in settings

## Code Standards

### Kotlin Style
- Follow official Kotlin style guide
- Use meaningful variable names
- Document public APIs
- Prefer immutability

### Architecture Patterns
- MVVM for UI components
- Repository pattern for data access
- Dependency injection (future with Hilt/Koin)

### Commit Messages
- Use conventional commits format
- Reference issue numbers
- Clear and descriptive messages
