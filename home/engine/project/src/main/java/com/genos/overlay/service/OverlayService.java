package com.genos.overlay.service;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.genos.overlay.R;
import com.genos.overlay.model.GeminiActionPlan;
import com.genos.overlay.model.GenosStatus;

/**
 * WindowManager-based overlay service for displaying GENOS status and controls
 */
public class OverlayService {
    private static final String TAG = "OverlayService";
    private static final int OVERLAY_REQUEST_CODE = 1234;
    private static final int OVERLAY_HEIGHT_DP = 200;
    private static final int OVERLAY_WIDTH_DP = 300;

    private Context context;
    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams overlayParams;
    private boolean isOverlayVisible = false;
    private boolean isOverlayPermissionGranted = false;

    // UI Components
    private TextView statusTextView;
    private TextView appTextView;
    private TextView geminiDecisionTextView;
    private ToggleButton automationToggleButton;
    private Button executeMockPlanButton;
    private Button injectTestCommandButton;
    private LinearLayout dragHandle;

    // Status tracking
    private GenosStatus currentStatus;
    private OverlayCallback callback;

    public interface OverlayCallback {
        void onAutomationToggled(boolean enabled);
        void onMockPlanExecutionRequested();
        void onTestCommandInjectionRequested();
        void onOverlayToggled(boolean visible);
    }

    public OverlayService(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.currentStatus = new GenosStatus();
        this.currentStatus.updateCurrentApp(context);
    }

    /**
     * Check if overlay permission is granted
     */
    public boolean isOverlayPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true; // For pre-M devices, always allowed
    }

    /**
     * Request overlay permission if needed
     */
    public void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isOverlayPermissionGranted()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * Create and show the overlay
     */
    public void showOverlay() {
        if (isOverlayVisible) {
            Log.w(TAG, "Overlay already visible");
            return;
        }

        if (!isOverlayPermissionGranted()) {
            Log.e(TAG, "Overlay permission not granted");
            requestOverlayPermission();
            return;
        }

        try {
            // Create overlay view
            overlayView = createOverlayView();
            
            // Set up layout parameters
            overlayParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                            WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
            );

            overlayParams.gravity = Gravity.TOP | Gravity.END;
            overlayParams.x = 20;
            overlayParams.y = 100;

            // Add overlay to window
            windowManager.addView(overlayView, overlayParams);
            isOverlayVisible = true;

            Log.i(TAG, "Overlay shown successfully");

            // Update status
            currentStatus.setOverlayVisible(true);
            if (callback != null) {
                callback.onOverlayToggled(true);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error showing overlay", e);
        }
    }

    /**
     * Hide the overlay
     */
    public void hideOverlay() {
        if (!isOverlayVisible) {
            Log.w(TAG, "Overlay not visible");
            return;
        }

        try {
            if (overlayView != null) {
                windowManager.removeView(overlayView);
                overlayView = null;
            }
            isOverlayVisible = false;

            Log.i(TAG, "Overlay hidden successfully");

            // Update status
            currentStatus.setOverlayVisible(false);
            if (callback != null) {
                callback.onOverlayToggled(false);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error hiding overlay", e);
        }
    }

    /**
     * Toggle overlay visibility
     */
    public void toggleOverlay() {
        if (isOverlayVisible) {
            hideOverlay();
        } else {
            showOverlay();
        }
    }

    /**
     * Create the overlay view with UI components
     */
    private View createOverlayView() {
        LayoutInflater inflater = LayoutInflater.from(context);
        View overlayView = inflater.inflate(R.layout.overlay_layout, null);

        // Find UI components
        statusTextView = overlayView.findViewById(R.id.status_text);
        appTextView = overlayView.findViewById(R.id.app_text);
        geminiDecisionTextView = overlayView.findViewById(R.id.gemini_decision_text);
        automationToggleButton = overlayView.findViewById(R.id.automation_toggle);
        executeMockPlanButton = overlayView.findViewById(R.id.execute_mock_plan);
        injectTestCommandButton = overlayView.findViewById(R.id.inject_test_command);
        dragHandle = overlayView.findViewById(R.id.drag_handle);

        // Set up drag functionality
        setupDragFunctionality();

        // Set up button click listeners
        setupButtonListeners();

        // Initial status update
        updateOverlayDisplay();

        return overlayView;
    }

    /**
     * Set up drag functionality for the overlay
     */
    private void setupDragFunctionality() {
        if (dragHandle == null) return;

        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (overlayParams == null) return false;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = overlayParams.x;
                        initialY = overlayParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        overlayParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        overlayParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        
                        // Update position on screen
                        if (windowManager != null && overlayView != null) {
                            windowManager.updateViewLayout(overlayView, overlayParams);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * Set up button click listeners
     */
    private void setupButtonListeners() {
        // Automation toggle
        if (automationToggleButton != null) {
            automationToggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.i(TAG, "Automation toggled: " + isChecked);
                currentStatus.setAutomationEnabled(isChecked);
                updateOverlayDisplay();
                
                if (callback != null) {
                    callback.onAutomationToggled(isChecked);
                }
            });
        }

        // Execute mock plan button
        if (executeMockPlanButton != null) {
            executeMockPlanButton.setOnClickListener(v -> {
                Log.i(TAG, "Mock plan execution requested");
                if (callback != null) {
                    callback.onMockPlanExecutionRequested();
                }
            });
        }

        // Inject test command button
        if (injectTestCommandButton != null) {
            injectTestCommandButton.setOnClickListener(v -> {
                Log.i(TAG, "Test command injection requested");
                if (callback != null) {
                    callback.onTestCommandInjectionRequested();
                }
            });
        }
    }

    /**
     * Update the overlay display with current status
     */
    public void updateOverlayDisplay() {
        if (!isOverlayVisible || overlayView == null) return;

        try {
            // Update status text
            if (statusTextView != null) {
                statusTextView.setText(currentStatus.getStatusDisplayString());
            }

            // Update app info
            if (appTextView != null) {
                String appInfo = "App: ";
                if (currentStatus.getCurrentAppName() != null && !currentStatus.getCurrentAppName().isEmpty()) {
                    appInfo += currentStatus.getCurrentAppName();
                } else if (currentStatus.getCurrentAppPackage() != null && !currentStatus.getCurrentAppPackage().isEmpty()) {
                    appInfo += currentStatus.getCurrentAppPackage();
                } else {
                    appInfo += "Unknown";
                }
                appTextView.setText(appInfo);
            }

            // Update Gemini decision
            if (geminiDecisionTextView != null) {
                String decisionText = "Gemini: ";
                if (currentStatus.getGeminiDecision() != null) {
                    decisionText += currentStatus.getGeminiDecision().getDecision().getMessage();
                    if (currentStatus.getGeminiDecision().getSummaryText() != null) {
                        decisionText += " - " + currentStatus.getGeminiDecision().getSummaryText();
                    }
                } else {
                    decisionText += "No decision available";
                }
                geminiDecisionTextView.setText(decisionText);
            }

            // Update automation toggle
            if (automationToggleButton != null) {
                automationToggleButton.setChecked(currentStatus.isAutomationEnabled());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating overlay display", e);
        }
    }

    /**
     * Update the current GENOS status
     */
    public void updateStatus(GenosStatus newStatus) {
        this.currentStatus = newStatus;
        updateOverlayDisplay();
    }

    /**
     * Update only the Gemini decision summary
     */
    public void updateGeminiDecision(GenosStatus.GeminiDecisionSummary decision) {
        currentStatus.setGeminiDecision(decision);
        updateOverlayDisplay();
    }

    /**
     * Update current app information
     */
    public void updateCurrentAppInfo(String packageName, String appName, String activityName) {
        currentStatus.setCurrentAppPackage(packageName);
        currentStatus.setCurrentAppName(appName);
        currentStatus.setCurrentActivity(activityName);
        updateOverlayDisplay();
    }

    /**
     * Update system state
     */
    public void updateSystemState(GenosStatus.SystemState state, String message) {
        currentStatus.setSystemState(state);
        
        if (message != null) {
            GenosStatus.GeminiDecisionSummary decision = new GenosStatus.GeminiDecisionSummary();
            decision.setDecision(GenosStatus.GeminiDecision.READY);
            decision.setSummaryText(message);
            currentStatus.setGeminiDecision(decision);
        }
        
        updateOverlayDisplay();
    }

    /**
     * Set callback for overlay events
     */
    public void setCallback(OverlayCallback callback) {
        this.callback = callback;
    }

    /**
     * Get current overlay visibility status
     */
    public boolean isOverlayVisible() {
        return isOverlayVisible;
    }

    /**
     * Get current status
     */
    public GenosStatus getCurrentStatus() {
        return currentStatus;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        hideOverlay();
    }
}