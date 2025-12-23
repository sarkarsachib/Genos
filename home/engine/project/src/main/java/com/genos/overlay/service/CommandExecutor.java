package com.genos.overlay.service;

import android.content.Context;
import android.util.Log;

import com.genos.overlay.model.ActionCommand;
import com.genos.overlay.model.GeminiActionPlan;
import com.genos.overlay.model.GenosStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CommandExecutor that consumes Gemini action plans and executes them sequentially
 * with error reporting and safeguards (timeouts, user cancel)
 */
public class CommandExecutor {
    private static final String TAG = "CommandExecutor";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private Context context;
    private ExecutorService executorService;
    private InputEmulatorService inputEmulatorService;
    private AtomicBoolean isExecuting = new AtomicBoolean(false);
    private AtomicBoolean isCancelled = new AtomicBoolean(false);
    private AtomicInteger currentCommandIndex = new AtomicInteger(0);
    
    private GeminiActionPlan currentPlan;
    private ExecutionCallback executionCallback;
    private GenosStatus genosStatus;

    public interface ExecutionCallback {
        void onExecutionStarted(GeminiActionPlan plan);
        void onCommandStarted(ActionCommand command, int index, int total);
        void onCommandCompleted(ActionCommand command, int index, int total);
        void onCommandFailed(ActionCommand command, int index, int total, String error);
        void onExecutionCompleted(GeminiActionPlan plan, boolean success, List<String> errors);
        void onExecutionCancelled(GeminiActionPlan plan);
        void onExecutionProgress(GeminiActionPlan plan, int currentIndex, int total);
    }

    public CommandExecutor(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.genosStatus = new GenosStatus();
        this.genosStatus.updateCurrentApp(context);
    }

    /**
     * Execute a Gemini action plan
     */
    public CompletableFuture<Boolean> executePlan(GeminiActionPlan plan) {
        return CompletableFuture.supplyAsync(() -> {
            if (isExecuting.get()) {
                Log.w(TAG, "Already executing a plan, ignoring new request");
                return false;
            }

            if (plan == null || plan.getCommands() == null || plan.getCommands().isEmpty()) {
                Log.e(TAG, "Invalid action plan");
                return false;
            }

            try {
                // Initialize execution state
                isExecuting.set(true);
                isCancelled.set(false);
                currentCommandIndex.set(0);
                currentPlan = plan;

                // Update plan status
                plan.setStatus(GeminiActionPlan.ExecutionStatus.RUNNING);
                plan.setCurrentCommandIndex(0);

                // Update GENOS status
                updateGenosStatus(GenosStatus.SystemState.EXECUTING, 
                                "Executing Gemini action plan: " + plan.getDescription());

                // Notify callback
                if (executionCallback != null) {
                    executionCallback.onExecutionStarted(plan);
                }

                List<String> errors = new ArrayList<>();
                List<ActionCommand> commands = plan.getCommands();
                int totalCommands = commands.size();

                // Execute commands sequentially
                for (int i = 0; i < totalCommands; i++) {
                    if (isCancelled.get()) {
                        Log.i(TAG, "Execution cancelled by user");
                        plan.setStatus(GeminiActionPlan.ExecutionStatus.CANCELLED);
                        if (executionCallback != null) {
                            executionCallback.onExecutionCancelled(plan);
                        }
                        updateGenosStatus(GenosStatus.SystemState.IDLE, "Execution cancelled");
                        return false;
                    }

                    ActionCommand command = commands.get(i);
                    currentCommandIndex.set(i);
                    plan.setCurrentCommandIndex(i);

                    Log.i(TAG, "Executing command " + (i + 1) + "/" + totalCommands + ": " + command);

                    // Update GENOS status
                    updateGenosStatus(GenosStatus.SystemState.EXECUTING, 
                                    "Executing: " + command.getType().name());

                    // Notify callback
                    if (executionCallback != null) {
                        executionCallback.onCommandStarted(command, i, totalCommands);
                        executionCallback.onExecutionProgress(plan, i, totalCommands);
                    }

                    // Execute command with timeout
                    boolean success = executeCommandWithTimeout(command, DEFAULT_TIMEOUT_SECONDS);

                    if (success) {
                        Log.i(TAG, "Command completed successfully: " + command.getCommandId());
                        
                        // Update command status
                        command.setStatus(ActionCommand.CommandStatus.COMPLETED);
                        
                        if (executionCallback != null) {
                            executionCallback.onCommandCompleted(command, i, totalCommands);
                        }
                    } else {
                        Log.e(TAG, "Command failed: " + command.getCommandId());
                        String error = "Command failed: " + command.getCommandId() + 
                                     (command.getErrorMessage() != null ? " - " + command.getErrorMessage() : "");
                        errors.add(error);

                        // Update command status
                        command.setStatus(ActionCommand.CommandStatus.FAILED);
                        
                        if (executionCallback != null) {
                            executionCallback.onCommandFailed(command, i, totalCommands, error);
                        }

                        // Decide whether to continue or stop based on command type
                        if (!shouldContinueOnError(command)) {
                            Log.w(TAG, "Stopping execution due to critical command failure");
                            break;
                        }
                    }

                    // Small delay between commands for stability
                    Thread.sleep(100);
                }

                // Finalize execution
                boolean finalSuccess = !isCancelled.get() && errors.isEmpty();
                if (finalSuccess) {
                    plan.setStatus(GeminiActionPlan.ExecutionStatus.COMPLETED);
                    updateGenosStatus(GenosStatus.SystemState.IDLE, "Execution completed successfully");
                } else {
                    plan.setStatus(GeminiActionPlan.ExecutionStatus.FAILED);
                    updateGenosStatus(GenosStatus.SystemState.ERROR, "Execution failed with " + errors.size() + " errors");
                }

                if (executionCallback != null) {
                    executionCallback.onExecutionCompleted(plan, finalSuccess, errors);
                }

                return finalSuccess;

            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during execution", e);
                
                if (currentPlan != null) {
                    currentPlan.setStatus(GeminiActionPlan.ExecutionStatus.FAILED);
                }
                
                List<String> errors = new ArrayList<>();
                errors.add("Unexpected error: " + e.getMessage());
                
                updateGenosStatus(GenosStatus.SystemState.ERROR, "Execution error: " + e.getMessage());

                if (executionCallback != null) {
                    executionCallback.onExecutionCompleted(currentPlan, false, errors);
                }

                return false;
            } finally {
                isExecuting.set(false);
                currentPlan = null;
                currentCommandIndex.set(0);
            }
        }, executorService);
    }

    /**
     * Execute a single command with timeout
     */
    private boolean executeCommandWithTimeout(ActionCommand command, int timeoutSeconds) {
        try {
            CompletableFuture<Boolean> future = inputEmulatorService.executeCommand(command, null);
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Command execution timeout or error", e);
            command.setErrorMessage(e.getMessage());
            return false;
        }
    }

    /**
     * Cancel current execution
     */
    public void cancelExecution() {
        if (isExecuting.get()) {
            Log.i(TAG, "Cancelling current execution");
            isCancelled.set(true);
            
            // Update GENOS status
            updateGenosStatus(GenosStatus.SystemState.IDLE, "Execution cancelled by user");
            
            if (executionCallback != null && currentPlan != null) {
                executionCallback.onExecutionCancelled(currentPlan);
            }
        }
    }

    /**
     * Check if a command failure should stop the entire execution
     */
    private boolean shouldContinueOnError(ActionCommand command) {
        // Continue on error for non-critical commands
        return command.getType() != ActionCommand.CommandType.BACK &&
               command.getType() != ActionCommand.CommandType.HOME &&
               command.getType() != ActionCommand.CommandType.RECENTS;
    }

    /**
     * Update GENOS system status
     */
    private void updateGenosStatus(GenosStatus.SystemState state, String message) {
        if (genosStatus != null) {
            genosStatus.setSystemState(state);
            genosStatus.updateCurrentApp(context);
            
            if (message != null) {
                GenosStatus.GeminiDecisionSummary geminiDecision = new GenosStatus.GeminiDecisionSummary();
                geminiDecision.setDecision(GenosStatus.GeminiDecision.READY);
                geminiDecision.setSummaryText(message);
                genosStatus.setGeminiDecision(geminiDecision);
            }
        }
    }

    /**
     * Execute a single command manually (for testing/debugging)
     */
    public CompletableFuture<Boolean> executeSingleCommand(ActionCommand command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                command.setExecutionStartTime(System.currentTimeMillis());
                boolean success = executeCommandWithTimeout(command, DEFAULT_TIMEOUT_SECONDS);
                command.setExecutionEndTime(System.currentTimeMillis());
                
                if (success) {
                    command.setStatus(ActionCommand.CommandStatus.COMPLETED);
                } else {
                    command.setStatus(ActionCommand.CommandStatus.FAILED);
                }
                
                return success;
            } catch (Exception e) {
                Log.e(TAG, "Error executing single command", e);
                command.setStatus(ActionCommand.CommandStatus.FAILED);
                command.setErrorMessage(e.getMessage());
                return false;
            }
        }, executorService);
    }

    /**
     * Get current execution status
     */
    public boolean isExecuting() {
        return isExecuting.get();
    }

    public boolean isCancelled() {
        return isCancelled.get();
    }

    public int getCurrentCommandIndex() {
        return currentCommandIndex.get();
    }

    public GeminiActionPlan getCurrentPlan() {
        return currentPlan;
    }

    public GenosStatus getGenosStatus() {
        return genosStatus;
    }

    /**
     * Set the execution callback
     */
    public void setExecutionCallback(ExecutionCallback callback) {
        this.executionCallback = callback;
    }

    /**
     * Set the input emulator service
     */
    public void setInputEmulatorService(InputEmulatorService inputEmulatorService) {
        this.inputEmulatorService = inputEmulatorService;
    }

    /**
     * Clean up resources
     */
    public void shutdown() {
        cancelExecution();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}