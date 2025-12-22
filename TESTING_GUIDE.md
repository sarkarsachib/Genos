# Genos Core - Testing Guide

## Quick Start

### Running All Tests
```bash
# Run all unit tests
./gradlew test

# Run all instrumentation tests
./gradlew connectedAndroidTest

# Run both unit and instrumentation tests
./gradlew test connectedAndroidTest
```

### Running Specific Test Classes

#### Unit Tests
```bash
# Application tests
./gradlew test --tests "ai.genos.core.GenosApplicationTest"
./gradlew test --tests "ai.genos.core.MainActivityUnitTest"

# Capture module tests
./gradlew test --tests "ai.genos.core.capture.ScreenCaptureManagerTest"
./gradlew test --tests "ai.genos.core.capture.TextRecognizerTest"

# Input module tests
./gradlew test --tests "ai.genos.core.input.InputProcessorTest"

# Overlay module tests
./gradlew test --tests "ai.genos.core.overlay.OverlayManagerTest"

# Service tests
./gradlew test --tests "ai.genos.core.service.GenosAccessibilityServiceTest"
./gradlew test --tests "ai.genos.core.service.InputProcessingServiceTest"
./gradlew test --tests "ai.genos.core.service.OverlayServiceTest"
./gradlew test --tests "ai.genos.core.service.ScreenCaptureServiceTest"

# UI tests
./gradlew test --tests "ai.genos.core.ui.SettingsActivityUnitTest"
./gradlew test --tests "ai.genos.core.ui.theme.*"
```

#### Instrumentation Tests
```bash
# MainActivity UI tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ai.genos.core.MainActivityTest

# SettingsActivity UI tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ai.genos.core.ui.SettingsActivityTest
```

### Running Tests by Module
```bash
# All capture module tests
./gradlew test --tests "ai.genos.core.capture.*"

# All service tests
./gradlew test --tests "ai.genos.core.service.*"

# All UI theme tests
./gradlew test --tests "ai.genos.core.ui.theme.*"
```

### Running with Coverage Reports
```bash
# Generate coverage report
./gradlew testDebugUnitTest jacocoTestReport

# View coverage report
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

## Test Structure

### Unit Tests Location