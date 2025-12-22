# GENOS Accessibility Core Service

A comprehensive Android accessibility service implementation for the GENOS platform that provides UI tree extraction, app context tracking, and event monitoring capabilities.

## Features

### Core Accessibility Service
- **Foreground Service**: Runs persistently with system notification
- **UI Tree Extraction**: Real-time extraction of UI hierarchy using AccessibilityNodeInfo
- **App Context Tracking**: Monitors current package, activity, and window changes
- **Event Monitoring**: Captures accessibility events (clicks, focus, scroll, etc.)
- **Event Bus**: Observer pattern for inter-module communication
- **Command Router**: API for downstream modules to interact with the service

### UI Tree Extraction
- Complete screen hierarchy as structured JSON-like DTOs
- Node bounds, accessibility attributes, and metadata
- Support for nested child nodes
- Efficient tree traversal and serialization

### Context Tracking
- Real-time package name and activity monitoring
- Window state changes detection
- App transition history
- Service lifecycle management

### Event Bus & Observer Pattern
- Accessibility event listeners
- App context change listeners
- UI tree change listeners
- Service state change listeners

### Command Router API
- Tree snapshot requests
- Node finding by text/ID
- Action execution (click, scroll, set text)
- Context queries
- Transition history

## Architecture

```
ai.genos.core.accessibility/
├── GenosAccessibilityService.kt          # Main accessibility service
├── AccessibilityServiceManager.kt        # Service state management
├── CommandRouter.kt                      # Command execution router
├── Models.kt                             # Data models (UiNode, UiTreeSnapshot, etc.)
├── Logger.kt                             # Logging utility
├── PermissionHelper.kt                   # Permission management
├── GenosForegroundService.kt            # Foreground service wrapper
├── MainActivity.kt                      # Demo/control activity
├── GenosAccessibilityApplication.kt     # Application class
├── BootReceiver.kt                      # Boot completed receiver
├── LogActivity.kt                       # Log viewer activity
└── BuildConfig.kt                       # Build configuration
```

## Data Models

### UiNode
Represents a single UI element in the accessibility tree:
- Node ID, class name, resource name
- Content description and text
- Accessibility properties (clickable, focusable, enabled, visible)
- Screen bounds and position
- Accessibility attributes
- Child nodes

### UiTreeSnapshot
Complete snapshot of the current UI state:
- Timestamp
- Package name and activity
- Window title
- Root UI node with full hierarchy

### AppContext
Current application context:
- Package name
- Activity name
- Window title
- Screen state
- Timestamp

### AppTransition
App transition event:
- From/to package and activity
- Timestamp
- Event type

## API Usage

### Getting UI Tree Snapshots
```kotlin
val service = GenosAccessibilityService.getInstance()
val treeSnapshot = service?.getCurrentUiTree()
if (treeSnapshot != null) {
    println("Current package: ${treeSnapshot.packageName}")
    println("UI tree: ${gson.toJson(treeSnapshot)}")
}
```

### Adding Event Listeners
```kotlin
val eventListener = object : AccessibilityEventListener {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        println("Event: ${event.eventType} in ${event.packageName}")
    }
}

service?.addEventListener(eventListener)
```

### Context Monitoring
```kotlin
val contextListener = object : AppContextListener {
    override fun onAppTransition(transition: AppTransition) {
        println("Transition: ${transition.fromPackage} -> ${transition.toPackage}")
    }
}

service?.addContextListener(contextListener)
```

### Executing Commands
```kotlin
val command = AccessibilityCommand(
    type = CommandType.FIND_NODE_BY_TEXT,
    parameters = mapOf("text" to "Login")
)

val result = service?.executeCommand(command)
if (result?.success == true) {
    println("Found nodes: ${result.data}")
}
```

### Service Management
```kotlin
val serviceManager = GenosAccessibilityService.getServiceManager()
val isRunning = serviceManager.isServiceRunning()
val recentTransitions = serviceManager.getRecentTransitions(10)
val stats = serviceManager.getServiceStatistics()
```

## Permission Setup

### Required Permissions
1. **Accessibility Service**: Must be enabled by user in system settings
2. **Foreground Service**: For persistent operation
3. **Overlay Permission**: For certain UI interactions (optional)

### Permission Helper Usage
```kotlin
// Check permissions
val status = PermissionHelper.getPermissionStatus(context)
if (status.allPermissionsGranted) {
    // Start service
} else {
    // Show setup dialog
    PermissionHelper.showAccessibilitySetupDialog(activity)
}

// Request specific permissions
PermissionHelper.requestAccessibilityService(activity)
PermissionHelper.requestOverlayPermission(activity)
```

## Service Configuration

### AndroidManifest.xml
The service is configured in the manifest with:
- Accessibility service intent filter
- Metadata configuration (XML)
- Required permissions
- Foreground service type

### accessibility_service_config.xml
Configuration includes:
- Event types to monitor
- Feedback type
- Flags for window content retrieval
- Notification timeout
- Capabilities (gestures, touch exploration)

## Testing

### Unit Tests
Located in `src/test/java/`:
- Service manager logic
- Data model validation
- Command router functionality
- Event listener interfaces

### Instrumentation Tests
Located in `src/androidTest/java/`:
- Service lifecycle testing
- UI tree extraction
- Tree serialization
- Permission handling
- Event flow verification

### Running Tests
```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest

# All tests
./gradlew check
```

## Service Lifecycle

### Starting the Service
1. User enables accessibility service in system settings
2. App requests foreground service permission
3. Service starts and connects to accessibility framework
4. Service begins monitoring events and extracting UI tree
5. Notification shown indicating active monitoring

### Monitoring Flow
1. Accessibility events received in `onAccessibilityEvent()`
2. Events processed and categorized
3. UI tree extracted when needed
4. Context changes detected and recorded
5. Event listeners notified
6. Transitions added to history

### Stopping the Service
1. User disables accessibility service
2. Service receives interrupt signal
3. Resources cleaned up
4. Service instance cleared
5. Notification removed

## Logging

### File Logging
- Logs stored in app-specific files directory
- Rotation to maintain file size limits
- Multiple log levels (INFO, DEBUG, WARN, ERROR)
- Timestamped entries with context

### Log Viewer
Built-in activity for viewing logs:
- Real-time log display
- Refresh and clear options
- Monospace formatting for readability

## Error Handling

### Service Recovery
- Automatic restart on crashes
- Health monitoring thread
- Graceful degradation on errors

### Permission Handling
- Graceful handling of disabled permissions
- User guidance for enabling services
- Clear error messages and recovery steps

## Security Considerations

- All accessibility data stays local to device
- No external transmission of UI content
- User consent required for accessibility access
- Minimal permission requests
- Secure command execution with validation

## Performance

- Efficient tree traversal algorithms
- Minimal memory allocation for tree extraction
- Background processing for event handling
- Optimized serialization with Gson
- Lazy loading of node attributes

## Future Enhancements

- Gesture simulation capabilities
- Advanced node filtering and search
- Performance metrics and profiling
- Network-based tree synchronization
- Machine learning for UI pattern recognition
- Accessibility analytics and insights