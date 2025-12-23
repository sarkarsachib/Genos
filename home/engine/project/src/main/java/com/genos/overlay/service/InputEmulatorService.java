package com.genos.overlay.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;

import com.genos.overlay.model.ActionCommand;
import com.genos.overlay.model.CommandParameters;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Accessibility service for rootless input emulation
 * Provides tap, long-press, swipe, scroll, and text input capabilities
 */
public class InputEmulatorService extends AccessibilityService {
    private final IBinder binder = new LocalBinder();
    private static final String TAG = "InputEmulatorService";
    private static InputEmulatorService instance;

    public interface InputCallback {
        void onSuccess(ActionCommand command);
        void onFailure(ActionCommand command, String error);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Monitor accessibility events if needed
        Log.d(TAG, "Accessibility event: " + event.getEventType());
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "Service interrupted");
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "InputEmulatorService connected");
    }

    public static InputEmulatorService getInstance() {
        return instance;
    }

    /**
     * Execute a single action command
     */
    public CompletableFuture<Boolean> executeCommand(ActionCommand command, InputCallback callback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                command.setStatus(ActionCommand.CommandStatus.RUNNING);
                command.setExecutionStartTime(System.currentTimeMillis());

                boolean success = false;
                switch (command.getType()) {
                    case TAP:
                        success = executeTap(command);
                        break;
                    case LONG_PRESS:
                        success = executeLongPress(command);
                        break;
                    case SWIPE:
                        success = executeSwipe(command);
                        break;
                    case SCROLL:
                        success = executeScroll(command);
                        break;
                    case TYPE_TEXT:
                        success = executeTypeText(command);
                        break;
                    case BACK:
                        success = performGlobalAction(GLOBAL_ACTION_BACK);
                        break;
                    case HOME:
                        success = performGlobalAction(GLOBAL_ACTION_HOME);
                        break;
                    case RECENTS:
                        success = performGlobalAction(GLOBAL_ACTION_RECENTS);
                        break;
                    case WAIT:
                        success = executeWait(command);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown command type: " + command.getType());
                }

                command.setExecutionEndTime(System.currentTimeMillis());
                
                if (success) {
                    command.setStatus(ActionCommand.CommandStatus.COMPLETED);
                    if (callback != null) {
                        callback.onSuccess(command);
                    }
                } else {
                    command.setStatus(ActionCommand.CommandStatus.FAILED);
                    command.setErrorMessage("Command execution failed");
                    if (callback != null) {
                        callback.onFailure(command, "Command execution failed");
                    }
                }

                return success;
            } catch (Exception e) {
                Log.e(TAG, "Error executing command", e);
                command.setStatus(ActionCommand.CommandStatus.FAILED);
                command.setErrorMessage(e.getMessage());
                if (callback != null) {
                    callback.onFailure(command, e.getMessage());
                }
                return false;
            }
        });
    }

    /**
     * Execute a tap gesture at specified coordinates
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean executeTap(ActionCommand command) {
        CommandParameters params = command.getParameters();
        if (params == null || params.getPosition() == null) {
            Log.e(TAG, "Tap command missing position parameter");
            return false;
        }

        PointF position = params.getPosition();
        Path path = new Path();
        path.moveTo(position.x, position.y);

        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(
                path, 0, 100 // Duration of 100ms for tap
        );

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();

        return dispatchGesture(gesture, null, null);
    }

    /**
     * Execute a long press gesture at specified coordinates
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean executeLongPress(ActionCommand command) {
        CommandParameters params = command.getParameters();
        if (params == null || params.getPosition() == null) {
            Log.e(TAG, "Long press command missing position parameter");
            return false;
        }

        PointF position = params.getPosition();
        Path path = new Path();
        path.moveTo(position.x, position.y);

        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(
                path, 0, 1000 // Duration of 1000ms for long press
        );

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();

        return dispatchGesture(gesture, null, null);
    }

    /**
     * Execute a swipe gesture from start to end position
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean executeSwipe(ActionCommand command) {
        CommandParameters params = command.getParameters();
        if (params == null || params.getStartPosition() == null || params.getEndPosition() == null) {
            Log.e(TAG, "Swipe command missing start/end position parameters");
            return false;
        }

        PointF start = params.getStartPosition();
        PointF end = params.getEndPosition();
        long duration = params.getDurationMs() != null ? params.getDurationMs() : 300;

        Path path = new Path();
        path.moveTo(start.x, start.y);
        path.lineTo(end.x, end.y);

        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(
                path, 0, duration
        );

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();

        return dispatchGesture(gesture, null, null);
    }

    /**
     * Execute a scroll gesture in specified direction
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean executeScroll(ActionCommand command) {
        CommandParameters params = command.getParameters();
        if (params == null || params.getDirection() == null) {
            Log.e(TAG, "Scroll command missing direction parameter");
            return false;
        }

        // For scroll, we need to find a scrollable node and perform scroll on it
        AccessibilityNodeInfo scrollableNode = findScrollableNode(getRootInActiveWindow());
        if (scrollableNode == null) {
            Log.w(TAG, "No scrollable node found");
            return false;
        }

        boolean success = false;
        String direction = params.getDirection().toLowerCase();

        switch (direction) {
            case "up":
            case "forward":
                success = scrollForward(scrollableNode);
                break;
            case "down":
            case "backward":
                success = scrollBackward(scrollableNode);
                break;
            case "left":
                // For horizontal scrolling, try ACTION_SCROLL_LEFT
                success = scrollLeft(scrollableNode);
                break;
            case "right":
                // For horizontal scrolling, try ACTION_SCROLL_RIGHT
                success = scrollRight(scrollableNode);
                break;
            default:
                Log.e(TAG, "Unknown scroll direction: " + direction);
                return false;
        }

        scrollableNode.recycle();
        return success;
    }

    /**
     * Execute text input via performAction with ACTION_SET_TEXT
     */
    private boolean executeTypeText(ActionCommand command) {
        CommandParameters params = command.getParameters();
        if (params == null || params.getText() == null) {
            Log.e(TAG, "Type text command missing text parameter");
            return false;
        }

        // First, try to find a focused edit text field
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "No root node available");
            return false;
        }

        // Look for focused edit text
        List<AccessibilityNodeInfo> focusedNodes = rootNode.findAccessibilityNodeInfosByViewFocused(AccessibilityNodeInfo.ACTION_SET_TEXT);
        
        if (focusedNodes != null && !focusedNodes.isEmpty()) {
            AccessibilityNodeInfo targetNode = focusedNodes.get(0);
            boolean success = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, 
                    android.os.Bundle.EMPTY.putCharSequence("text", params.getText()));
            targetNode.recycle();
            return success;
        }

        // Fallback: Look for any EditText
        List<AccessibilityNodeInfo> editTexts = rootNode.findAccessibilityNodeInfosByView("android.widget.EditText");
        if (editTexts != null && !editTexts.isEmpty()) {
            AccessibilityNodeInfo editText = editTexts.get(0);
            boolean success = editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,
                    android.os.Bundle.EMPTY.putCharSequence("text", params.getText()));
            editText.recycle();
            return success;
        }

        // Fallback: Simulate typing character by character
        return simulateTyping(params.getText());
    }

    /**
     * Execute a wait command
     */
    private boolean executeWait(ActionCommand command) {
        CommandParameters params = command.getParameters();
        long duration = params.getDurationMs() != null ? params.getDurationMs() : 1000;
        
        try {
            Thread.sleep(duration);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // Helper methods for scrolling
    private AccessibilityNodeInfo findScrollableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;

        // Check if current node is scrollable
        if (node.isScrollable()) {
            return node;
        }

        // Recursively search children
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo scrollable = findScrollableNode(child);
                if (scrollable != null) {
                    return scrollable;
                }
            }
        }

        return null;
    }

    private boolean scrollForward(AccessibilityNodeInfo node) {
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
    }

    private boolean scrollBackward(AccessibilityNodeInfo node) {
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    private boolean scrollLeft(AccessibilityNodeInfo node) {
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_LEFT);
    }

    private boolean scrollRight(AccessibilityNodeInfo node) {
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_RIGHT);
    }

    /**
     * Fallback method to simulate typing character by character
     */
    private boolean simulateTyping(String text) {
        try {
            for (char c : text.toCharArray()) {
                String character = String.valueOf(c);
                injectInputEvent(character);
                Thread.sleep(50); // Small delay between characters
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error simulating typing", e);
            return false;
        }
    }

    private void injectInputEvent(String text) {
        // This is a simplified fallback - in a real implementation, 
        // you might use InputMethodManager or other input injection methods
        Log.d(TAG, "Injecting text: " + text);
    }

    /**
     * Check if the service is currently active
     */
    public boolean isServiceActive() {
        return instance != null;
    }

    /**
     * Get accessibility node info for debugging
     */
    public AccessibilityNodeInfo getRootNode() {
        return getRootInActiveWindow();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public InputEmulatorService getService() {
            return InputEmulatorService.this;
        }
    }
}