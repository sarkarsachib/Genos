package com.genos.overlay.model;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import java.util.List;
import java.util.Objects;

/**
 * Represents the current status of the GENOS system
 */
public class GenosStatus {
    private SystemState systemState;
    private String currentAppPackage;
    private String currentAppName;
    private String currentActivity;
    private GeminiDecisionSummary geminiDecision;
    private boolean automationEnabled;
    private boolean overlayVisible;
    private long lastUpdated;
    private String version;
    private List<String> supportedActions;

    public GenosStatus() {
        this.systemState = SystemState.IDLE;
        this.lastUpdated = System.currentTimeMillis();
        this.version = "1.0.0";
        this.automationEnabled = false;
        this.overlayVisible = false;
    }

    public enum SystemState {
        IDLE("Idle"),
        ANALYZING("Analyzing UI"),
        EXECUTING("Executing Actions"),
        WAITING("Waiting for Input"),
        ERROR("Error State"),
        DISABLED("Disabled");

        private final String displayName;

        SystemState(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum GeminiDecision {
        PENDING("Gemini decision pending"),
        ANALYZING("Gemini analyzing screen"),
        READY("Ready for execution"),
        EXECUTING("Gemini actions executing"),
        COMPLETED("Gemini actions completed"),
        FAILED("Gemini decision failed");

        private final String message;

        GeminiDecision(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Summary of Gemini's decision and planned actions
     */
    public static class GeminiDecisionSummary {
        private GeminiDecision decision;
        private String summaryText;
        private int plannedActions;
        private String confidence;
        private String reasoning;

        public GeminiDecisionSummary() {
            this.decision = GeminiDecision.PENDING;
            this.plannedActions = 0;
            this.confidence = "Unknown";
        }

        public GeminiDecisionSummary(GeminiDecision decision, String summaryText, int plannedActions) {
            this.decision = decision;
            this.summaryText = summaryText;
            this.plannedActions = plannedActions;
            this.confidence = "High";
        }

        // Getters and Setters
        public GeminiDecision getDecision() {
            return decision;
        }

        public void setDecision(GeminiDecision decision) {
            this.decision = decision;
        }

        public String getSummaryText() {
            return summaryText;
        }

        public void setSummaryText(String summaryText) {
            this.summaryText = summaryText;
        }

        public int getPlannedActions() {
            return plannedActions;
        }

        public void setPlannedActions(int plannedActions) {
            this.plannedActions = plannedActions;
        }

        public String getConfidence() {
            return confidence;
        }

        public void setConfidence(String confidence) {
            this.confidence = confidence;
        }

        public String getReasoning() {
            return reasoning;
        }

        public void setReasoning(String reasoning) {
            this.reasoning = reasoning;
        }

        @Override
        public String toString() {
            return "GeminiDecisionSummary{" +
                    "decision=" + decision +
                    ", summaryText='" + summaryText + '\'' +
                    ", plannedActions=" + plannedActions +
                    ", confidence='" + confidence + '\'' +
                    ", reasoning='" + reasoning + '\'' +
                    '}';
        }
    }

    // Getters and Setters
    public SystemState getSystemState() {
        return systemState;
    }

    public void setSystemState(SystemState systemState) {
        this.systemState = systemState;
        updateTimestamp();
    }

    public String getCurrentAppPackage() {
        return currentAppPackage;
    }

    public void setCurrentAppPackage(String currentAppPackage) {
        this.currentAppPackage = currentAppPackage;
        updateTimestamp();
    }

    public String getCurrentAppName() {
        return currentAppName;
    }

    public void setCurrentAppName(String currentAppName) {
        this.currentAppName = currentAppName;
        updateTimestamp();
    }

    public String getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(String currentActivity) {
        this.currentActivity = currentActivity;
        updateTimestamp();
    }

    public GeminiDecisionSummary getGeminiDecision() {
        return geminiDecision;
    }

    public void setGeminiDecision(GeminiDecisionSummary geminiDecision) {
        this.geminiDecision = geminiDecision;
        updateTimestamp();
    }

    public boolean isAutomationEnabled() {
        return automationEnabled;
    }

    public void setAutomationEnabled(boolean automationEnabled) {
        this.automationEnabled = automationEnabled;
        updateTimestamp();
    }

    public boolean isOverlayVisible() {
        return overlayVisible;
    }

    public void setOverlayVisible(boolean overlayVisible) {
        this.overlayVisible = overlayVisible;
        updateTimestamp();
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getSupportedActions() {
        return supportedActions;
    }

    public void setSupportedActions(List<String> supportedActions) {
        this.supportedActions = supportedActions;
    }

    private void updateTimestamp() {
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * Updates the current app information from the system
     */
    public void updateCurrentApp(Context context) {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
                if (!tasks.isEmpty()) {
                    ActivityManager.RunningTaskInfo task = tasks.get(0);
                    if (task.topActivity != null) {
                        currentAppPackage = task.topActivity.getPackageName();
                        currentActivity = task.topActivity.getClassName();
                    }
                }
            }
        } catch (Exception e) {
            // Handle permission issues gracefully
            currentAppPackage = "Unknown";
            currentActivity = "Unknown";
        }
        updateTimestamp();
    }

    /**
     * Creates a simplified status string for display
     */
    public String getStatusDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GENOS ").append(version);
        sb.append(" | ").append(systemState.getDisplayName());
        
        if (currentAppName != null && !currentAppName.isEmpty()) {
            sb.append(" | ").append(currentAppName);
        } else if (currentAppPackage != null && !currentAppPackage.isEmpty()) {
            sb.append(" | ").append(currentAppPackage);
        }
        
        if (geminiDecision != null) {
            sb.append(" | ").append(geminiDecision.getDecision().getMessage());
        }
        
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenosStatus that = (GenosStatus) o;
        return automationEnabled == that.automationEnabled &&
                overlayVisible == that.overlayVisible &&
                systemState == that.systemState &&
                Objects.equals(currentAppPackage, that.currentAppPackage) &&
                Objects.equals(currentAppName, that.currentAppName) &&
                Objects.equals(currentActivity, that.currentActivity) &&
                Objects.equals(geminiDecision, that.geminiDecision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(systemState, currentAppPackage, currentAppName, currentActivity,
                           geminiDecision, automationEnabled, overlayVisible);
    }

    @Override
    public String toString() {
        return "GenosStatus{" +
                "systemState=" + systemState +
                ", currentAppPackage='" + currentAppPackage + '\'' +
                ", currentAppName='" + currentAppName + '\'' +
                ", currentActivity='" + currentActivity + '\'' +
                ", geminiDecision=" + geminiDecision +
                ", automationEnabled=" + automationEnabled +
                ", overlayVisible=" + overlayVisible +
                ", version='" + version + '\'' +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}