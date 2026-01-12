package com.genos.overlay.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents a Gemini action plan with sequence of commands to execute
 */
public class GeminiActionPlan {
    private String planId;
    private String description;
    private List<ActionCommand> commands;
    private long createdAt;
    private int currentCommandIndex;
    private ExecutionStatus status;

    public GeminiActionPlan() {
        this.createdAt = System.currentTimeMillis();
        this.currentCommandIndex = 0;
        this.status = ExecutionStatus.PENDING;
    }

    public GeminiActionPlan(String planId, String description, List<ActionCommand> commands) {
        this();
        this.planId = planId;
        this.description = description;
        this.commands = commands;
    }

    public enum ExecutionStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    // Getters and Setters
    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ActionCommand> getCommands() {
        return commands;
    }

    public void setCommands(List<ActionCommand> commands) {
        this.commands = commands;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getCurrentCommandIndex() {
        return currentCommandIndex;
    }

    public void setCurrentCommandIndex(int currentCommandIndex) {
        this.currentCommandIndex = currentCommandIndex;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public ActionCommand getCurrentCommand() {
        if (commands != null && currentCommandIndex < commands.size()) {
            return commands.get(currentCommandIndex);
        }
        return null;
    }

    public boolean hasNextCommand() {
        return commands != null && currentCommandIndex < commands.size() - 1;
    }

    public ActionCommand getNextCommand() {
        if (hasNextCommand()) {
            return commands.get(currentCommandIndex + 1);
        }
        return null;
    }

    public void advanceToNextCommand() {
        if (hasNextCommand()) {
            currentCommandIndex++;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeminiActionPlan that = (GeminiActionPlan) o;
        return Objects.equals(planId, that.planId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(planId);
    }

    @Override
    public String toString() {
        return "GeminiActionPlan{" +
                "planId='" + planId + '\'' +
                ", description='" + description + '\'' +
                ", commands=" + commands +
                ", status=" + status +
                ", currentCommandIndex=" + currentCommandIndex +
                '}';
    }
}