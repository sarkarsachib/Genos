# GENOS Overlay System Testing Guide

## Prerequisites
- Android device/emulator with API level 26+ (Android 8.0)
- App installed with all permissions granted
- Accessibility service enabled for the app
- Overlay permission granted

## Manual Testing Steps

### 1. Initial Setup
1. **Install the app** with `adb install app-debug.apk`
2. **Launch the app** from the app drawer
3. **Grant overlay permission**: Tap "Enable Overlay & Accessibility Permissions"
4. **Enable accessibility service**: Go to Settings → Accessibility → GENOS Accessibility Service → Enable
5. **Return to the app** and confirm overlay permission is granted

### 2. Overlay Launch Test
1. **Tap "Launch GENOS Overlay"** button
2. **Verify overlay appears** as a floating window on top of the app
3. **Observe overlay content**:
   - GENOS status showing "GENOS: Monitoring"
   - Current app context (e.g., "AndroidProject (com.example.androidproject)")
   - Three control buttons: Start/Stop, OCR, Tree

### 3. Overlay Persistence Test
1. **Press Home button** to go to launcher
2. **Verify overlay remains visible** on top of the launcher
3. **Open another app** (e.g., Settings)
4. **Verify overlay remains visible** and updates app context to show current app
5. **Tap overlay buttons** to confirm they still respond

### 4. Command Execution Test
1. **Go back to GENOS app**
2. **Tap "Test Tap" button**
3. **Observe overlay response**:
   - Status updates to "Command: Tap at (500, 500)"
   - Visual feedback shows pink ripple at coordinates (500, 500)
   - Touch visualization appears for 2 seconds then fades

### 5. OCR Functionality Test
1. **Navigate to an app with text** (e.g., Settings)
2. **Tap the "OCR" button** in overlay
3. **Observe overlay response**:
   - Status updates to "GENOS: Performing OCR..."
   - After completion, status shows "GENOS: OCR Complete"
   - OCR text appears in overlay under "OCR Result:"

### 6. Automation Control Test
1. **In overlay, tap "Start" button**
2. **Verify button changes to "Stop"** and turns red
3. **Navigate between different apps**
4. **Verify app context updates** in overlay automatically
5. **Tap "Stop" button**
6. **Verify button changes back to "Start"** and turns primary color

### 7. UI Tree Display Test
1. **Tap "Tree" button** in overlay
2. **Observe UI tree panel** appears in overlay with accessibility node information
3. **Verify tree auto-hides after 5 seconds** or tap "Tree" again to toggle visibility

### 8. Touch Visualization Test
1. **Execute tap command**: Enter "tap 300 800" and tap "Send Command"
2. **Observe overlay shows**: "Executing: tap 300 800" and pink ripple effect
3. **Execute swipe visualization**: Enter "swipe 100 1000 700 1000" and send
4. **Observe green start circle** and red end circle with connecting line
5. **Test scroll visualization**: Use "scroll down" command
6. **Observe multiple blue lines** indicating scroll gesture

### 9. Integration Pipeline Test
**Complete end-to-end workflow:**
1. **Start in Settings app**
2. **Tap "Tree" button** to capture UI hierarchy
3. **Identify a tap target** from the UI tree (e.g., a button or menu item)
4. **Execute tap command** on those coordinates
5. **Verify action was executed** (Settings screen changed)
6. **Tap "OCR" button** to capture screen text
7. **Verify OCR results** appear in overlay

### 10. Edge Cases and Error Handling
1. **Invalid commands**: Enter "invalid command" - should show no action
2. **Out of bounds coordinates**: Try "tap -100 5000" - should handle gracefully
3. **Rapid commands**: Send multiple commands quickly - should queue appropriately
4. **Service restart**: Stop and restart overlay service - should recover state
5. **Device rotation**: Rotate device while overlay is visible - should maintain position

### 11. Performance Tests
1. **Extended use**: Leave overlay running for 30+ minutes
2. **Memory usage**: Monitor app memory in Settings → Apps → GENOS
3. **Battery consumption**: Check battery usage after extended operation
4. **Responsiveness**: Verify overlay remains responsive during heavy device use

## Test Verification Checklist

- [ ] Overlay appears on top of all apps
- [ ] App context updates automatically when switching apps
- [ ] Start/Stop button controls automation state
- [ ] OCR button triggers text recognition and displays results  
- [ ] Tree button shows/hides UI hierarchy
- [ ] Tap commands show visual feedback with pink ripple
- [ ] Swipe commands show start/end points and path
- [ ] Scroll commands show directional arrows
- [ ] All commands update status display in overlay
- [ ] Overlay persists across app changes and device rotation
- [ ] No crashes or ANRs during extended use
- [ ] Integration demo successfully completes full pipeline

## Automated Test Execution

Run integration tests with:

```bash
./gradlew connectedAndroidTest --tests "com.example.androidproject.overlay.OverlayIntegrationTest"
```

Run UI tests with:

```bash
./gradlew connectedAndroidTest --tests "com.example.androidproject.overlay.OverlayUiTest"
```

## Troubleshooting

### Overlay not visible
- Verify overlay permission is granted in Settings → Apps → Special app access → Display over other apps

### Commands not executing
- Ensure accessibility service is enabled in Settings → Accessibility → GENOS Accessibility Service

### No OCR results
- Check Google Play Services is updated for ML Kit

### Touch visualization not appearing
- Verify commands are being sent to OverlayService via broadcast intents

## Success Criteria

All acceptance criteria are met when:
1. ✅ Overlay appears atop all apps with TYPE_APPLICATION_OVERLAY
2. ✅ Can be toggled on/off via visibility controls
3. ✅ Highlights actions being executed with visual feedback
4. ✅ Integration demo performs screen read → command → execute → feedback pipeline
5. ✅ Comprehensive tests validate end-to-end functionality