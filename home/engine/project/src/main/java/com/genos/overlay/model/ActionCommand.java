package com.genos.overlay.model;

import android.graphics.PointF;
import android.view.InputDevice;
import android.view.MotionEvent;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Represents a single action command (tap, swipe, type, etc.)
 */
public class ActionCommand {
    private String commandId;
    private CommandType type;
    private CommandParameters parameters;
    private long timeoutMs;
    private CommandStatus status;
    private String errorMessage;
    private long executionStartTime;
    private long executionEndTime;

    public ActionCommand() {
        this.timeoutMs = TimeUnit.SECONDS.toMillis(5);
        this.status = CommandStatus.PENDING;
    }

    public ActionCommand(String commandId, CommandType type, CommandParameters parameters) {
        this();
        this.commandId = commandId;
        this.type = type;
        this.parameters = parameters;
    }

    public enum CommandType {
        TAP("tap"),
        LONG_PRESS("long_press"),
        SWIPE("swipe"),
        SCROLL("scroll"),
        TYPE_TEXT("type_text"),
        WAIT("wait"),
        BACK("back"),
        HOME("home"),
        RECENTS("recents");

        private final String value;

        CommandType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static CommandType fromString(String value) {
            for (CommandType type : CommandType.values()) {
                if (type.getValue().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown command type: " + value);
        }
    }

    public enum CommandStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    // Getters and Setters
    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public CommandType getType() {
        return type;
    }

    public void setType(CommandType type) {
        this.type = type;
    }

    public CommandParameters getParameters() {
        return parameters;
    }

    public void setParameters(CommandParameters parameters) {
        this.parameters = parameters;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public CommandStatus getStatus() {
        return status;
    }

    public void setStatus(CommandStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getExecutionStartTime() {
        return executionStartTime;
    }

    public void setExecutionStartTime(long executionStartTime) {
        this.executionStartTime = executionStartTime;
    }

    public long getExecutionEndTime() {
        return executionEndTime;
    }

    public void setExecutionEndTime(long executionEndTime) {
        this.executionEndTime = executionEndTime;
    }

    public long getExecutionDuration() {
        if (executionStartTime > 0 && executionEndTime > 0) {
            return executionEndTime - executionStartTime;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionCommand that = (ActionCommand) o;
        return Objects.equals(commandId, that.commandId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandId);
    }

    @Override
    public String toString() {
        return "ActionCommand{" +
                "commandId='" + commandId + '\'' +
                ", type=" + type +
                ", parameters=" + parameters +
                ", status=" + status +
                '}';
    }
}