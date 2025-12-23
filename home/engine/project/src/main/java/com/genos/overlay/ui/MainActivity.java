package com.genos.overlay.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.genos.overlay.R;
import com.genos.overlay.model.ActionCommand;
import com.genos.overlay.model.CommandParameters;
import com.genos.overlay.model.GeminiActionPlan;
import com.genos.overlay.model.GenosStatus;
import com.genos.overlay.service.CommandExecutor;
import com.genos.overlay.service.InputEmulatorService;
import com.genos.overlay.service.OverlayService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MainActivity providing debug UI for testing the overlay, input emulator, and command executor
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int OVERLAY_PERMISSION_REQUEST = 1001;
    private static final int ACCESSIBILITY_PERMISSION_REQUEST = 1002;

    // Services
    private OverlayService overlayService;
    private InputEmulatorService inputEmulatorService;
    private CommandExecutor commandExecutor;

    // UI Components
    private Switch overlaySwitch;
    private Switch automationSwitch;
    private Switch accessibilitySwitch;
    private TextView statusTextView;
    private Button executeMockPlanButton;
    private Button injectTapCommandButton;
    private Button injectSwipeCommandButton;
    private Button injectTypeCommandButton;
    private EditText customCommandEditText;
    private Button injectCustomCommandButton;
    private RecyclerView commandLogRecyclerView;
    private CommandLogAdapter commandLogAdapter;

    // State tracking
    private boolean isOverlayServiceBound = false;
    private boolean isAccessibilityServiceConnected = false;
    private List<CommandLogEntry> commandLog = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        checkPermissions();
        initializeServices();
    }

    private void initializeViews() {
        overlaySwitch = findViewById(R.id.overlay_switch);
        automationSwitch = findViewById(R.id.automation_switch);
        accessibilitySwitch = findViewById(R.id.accessibility_switch);
        statusTextView = findViewById(R.id.status_text);
        executeMockPlanButton = findViewById(R.id.execute_mock_plan_button);
        injectTapCommandButton = findViewById(R.id.inject_tap_command_button);
        injectSwipeCommandButton = findViewById(R.id.inject_swipe_command_button);
        injectTypeCommandButton = findViewById(R.id.inject_type_command_button);
        customCommandEditText = findViewById(R.id.custom_command_edit_text);
        injectCustomCommandButton = findViewById(R.id.inject_custom_command_button);
        commandLogRecyclerView = findViewById(R.id.command_log_recycler_view);
    }

    private void setupRecyclerView() {
        commandLogAdapter = new CommandLogAdapter(commandLog);
        commandLogRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commandLogRecyclerView.setAdapter(commandLogAdapter);
    }

    private void setupClickListeners() {
        // Overlay toggle
        overlaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.i(TAG, "Overlay toggle: " + isChecked);
            if (isChecked) {
                showOverlay();
            } else {
                hideOverlay();
            }
        });

        // Automation toggle
        automationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.i(TAG, "Automation toggle: " + isChecked);
            if (overlayService != null) {
                GenosStatus status = overlayService.getCurrentStatus();
                status.setAutomationEnabled(isChecked);
                overlayService.updateOverlayDisplay();
            }
        });

        // Accessibility service check
        accessibilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkAccessibilityService();
            } else {
                // Cannot disable accessibility service programmatically
                unbindFromAccessibilityService();
                Toast.makeText(this, "Service disconnected", Toast.LENGTH_SHORT).show();
                accessibilitySwitch.setChecked(false);
            }
        });

        // Execute mock plan
        executeMockPlanButton.setOnClickListener(v -> executeMockGeminiPlan());

        // Test commands
        injectTapCommandButton.setOnClickListener(v -> injectTestTap());
        injectSwipeCommandButton.setOnClickListener(v -> injectTestSwipe());
        injectTypeCommandButton.setOnClickListener(v -> injectTestTypeText());
        injectCustomCommandButton.setOnClickListener(v -> injectCustomCommand());
    }

    private void checkPermissions() {
        // Check overlay permission
        boolean hasOverlayPermission = overlayService != null && overlayService.isOverlayPermissionGranted();
        overlaySwitch.setEnabled(hasOverlayPermission);
        overlaySwitch.setChecked(hasOverlayPermission && overlayService.isOverlayVisible());

        // Check accessibility service
        boolean hasAccessibilityService = isAccessibilityServiceEnabled();
        accessibilitySwitch.setEnabled(hasAccessibilityService);
        accessibilitySwitch.setChecked(hasAccessibilityService);
    }

    private void initializeServices() {
        // Initialize overlay service
        overlayService = new OverlayService(this);
        overlayService.setCallback(new OverlayService.OverlayCallback() {
            @Override
            public void onAutomationToggled(boolean enabled) {
                runOnUiThread(() -> {
                    automationSwitch.setChecked(enabled);
                    addLogEntry("Automation " + (enabled ? "enabled" : "disabled"));
                });
            }

            @Override
            public void onMockPlanExecutionRequested() {
                runOnUiThread(() -> executeMockGeminiPlan());
            }

            @Override
            public void onTestCommandInjectionRequested() {
                runOnUiThread(() -> injectTestTap());
            }

            @Override
            public void onOverlayToggled(boolean visible) {
                runOnUiThread(() -> {
                    overlaySwitch.setChecked(visible);
                    addLogEntry("Overlay " + (visible ? "shown" : "hidden"));
                });
            }
        });

        // Initialize command executor
        commandExecutor = new CommandExecutor(this);
        commandExecutor.setExecutionCallback(new CommandExecutor.ExecutionCallback() {
            @Override
            public void onExecutionStarted(GeminiActionPlan plan) {
                addLogEntry("Started execution: " + plan.getDescription());
            }

            @Override
            public void onCommandStarted(ActionCommand command, int index, int total) {
                addLogEntry("Command " + (index + 1) + "/" + total + ": " + command.getType().name());
            }

            @Override
            public void onCommandCompleted(ActionCommand command, int index, int total) {
                addLogEntry("✓ Command completed: " + command.getCommandId());
            }

            @Override
            public void onCommandFailed(ActionCommand command, int index, int total, String error) {
                addLogEntry("✗ Command failed: " + command.getCommandId() + " - " + error);
            }

            @Override
            public void onExecutionCompleted(GeminiActionPlan plan, boolean success, List<String> errors) {
                addLogEntry("Execution " + (success ? "completed successfully" : "failed"));
                if (!errors.isEmpty()) {
                    addLogEntry("Errors: " + errors.size());
                }
            }

            @Override
            public void onExecutionCancelled(GeminiActionPlan plan) {
                addLogEntry("Execution cancelled by user");
            }

            @Override
            public void onExecutionProgress(GeminiActionPlan plan, int currentIndex, int total) {
                // Update UI with progress if needed
            }
        });
    }

    private void showOverlay() {
        if (!overlayService.isOverlayPermissionGranted()) {
            overlayService.requestOverlayPermission();
            Toast.makeText(this, "Please grant overlay permission", Toast.LENGTH_LONG).show();
            overlaySwitch.setChecked(false);
            return;
        }

        overlayService.showOverlay();
        updateStatusText("Overlay shown");
    }

    private void hideOverlay() {
        overlayService.hideOverlay();
        updateStatusText("Overlay hidden");
    }

    private void checkAccessibilityService() {
        if (!isAccessibilityServiceEnabled()) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(intent, ACCESSIBILITY_PERMISSION_REQUEST);
        } else {
            // Bind to accessibility service
            bindToAccessibilityService();
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String packageName = getPackageName();
        String serviceName = InputEmulatorService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        
        return enabledServices != null && enabledServices.contains(serviceName);
    }

    private void executeMockGeminiPlan() {
        addLogEntry("Executing mock Gemini plan...");

        // Create a mock Gemini action plan
        List<ActionCommand> commands = new ArrayList<>();
        
        // Mock plan: Open app drawer and tap first app
        commands.add(new ActionCommand("cmd1", ActionCommand.CommandType.HOME, null));
        commands.add(new ActionCommand("cmd2", ActionCommand.CommandType.WAIT, 
                CommandParameters.newBuilder().duration(1000L).build()));
        commands.add(new ActionCommand("cmd3", ActionCommand.CommandType.SWIPE,
                CommandParameters.newBuilder()
                        .startPosition(100f, 800f)
                        .endPosition(100f, 200f)
                        .duration(500L)
                        .build()));
        
        GeminiActionPlan plan = new GeminiActionPlan(
                UUID.randomUUID().toString(),
                "Mock plan: Navigate home and swipe up",
                commands
        );

        // Connect to accessibility service first
        if (inputEmulatorService == null) {
            addLogEntry("Accessibility service not connected");
            Toast.makeText(this, "Please enable accessibility service", Toast.LENGTH_SHORT).show();
            return;
        }

        commandExecutor.setInputEmulatorService(inputEmulatorService);
        commandExecutor.executePlan(plan);
    }

    private void injectTestTap() {
        addLogEntry("Injecting test tap...");

        ActionCommand tapCommand = new ActionCommand(
                "test_tap_" + System.currentTimeMillis(),
                ActionCommand.CommandType.TAP,
                CommandParameters.newBuilder()
                        .position(200f, 400f) // Center screen tap
                        .build()
        );

        if (inputEmulatorService == null) {
            addLogEntry("Accessibility service not connected");
            Toast.makeText(this, "Please enable accessibility service", Toast.LENGTH_SHORT).show();
            return;
        }

        commandExecutor.executeSingleCommand(tapCommand);
    }

    private void injectTestSwipe() {
        addLogEntry("Injecting test swipe...");

        ActionCommand swipeCommand = new ActionCommand(
                "test_swipe_" + System.currentTimeMillis(),
                ActionCommand.CommandType.SWIPE,
                CommandParameters.newBuilder()
                        .startPosition(100f, 500f)
                        .endPosition(300f, 300f)
                        .duration(400L)
                        .build()
        );

        if (inputEmulatorService == null) {
            addLogEntry("Accessibility service not connected");
            Toast.makeText(this, "Please enable accessibility service", Toast.LENGTH_SHORT).show();
            return;
        }

        commandExecutor.executeSingleCommand(swipeCommand);
    }

    private void injectTestTypeText() {
        String testText = "Hello GENOS!";
        addLogEntry("Injecting test text: " + testText);

        ActionCommand typeCommand = new ActionCommand(
                "test_type_" + System.currentTimeMillis(),
                ActionCommand.CommandType.TYPE_TEXT,
                CommandParameters.newBuilder()
                        .text(testText)
                        .build()
        );

        if (inputEmulatorService == null) {
            addLogEntry("Accessibility service not connected");
            Toast.makeText(this, "Please enable accessibility service", Toast.LENGTH_SHORT).show();
            return;
        }

        commandExecutor.executeSingleCommand(typeCommand);
    }

    private void injectCustomCommand() {
        String commandText = customCommandEditText.getText().toString().trim();
        if (commandText.isEmpty()) {
            Toast.makeText(this, "Please enter a command", Toast.LENGTH_SHORT).show();
            return;
        }

        addLogEntry("Injecting custom command: " + commandText);

        // Parse simple commands like "tap(x,y)", "swipe(x1,y1,x2,y2)", "type(text)"
        try {
            ActionCommand command = parseCustomCommand(commandText);
            if (command != null && inputEmulatorService != null) {
                commandExecutor.executeSingleCommand(command);
            } else {
                addLogEntry("Failed to parse command or accessibility service not connected");
            }
        } catch (Exception e) {
            addLogEntry("Error parsing command: " + e.getMessage());
        }
    }

    private ActionCommand parseCustomCommand(String commandText) {
        if (commandText.startsWith("tap(") && commandText.endsWith(")")) {
            String coords = commandText.substring(4, commandText.length() - 1);
            String[] parts = coords.split(",");
            if (parts.length == 2) {
                float x = Float.parseFloat(parts[0].trim());
                float y = Float.parseFloat(parts[1].trim());
                return new ActionCommand("custom_tap", ActionCommand.CommandType.TAP,
                        CommandParameters.newBuilder().position(x, y).build());
            }
        } else if (commandText.startsWith("swipe(") && commandText.endsWith(")")) {
            String coords = commandText.substring(6, commandText.length() - 1);
            String[] parts = coords.split(",");
            if (parts.length == 4) {
                float x1 = Float.parseFloat(parts[0].trim());
                float y1 = Float.parseFloat(parts[1].trim());
                float x2 = Float.parseFloat(parts[2].trim());
                float y2 = Float.parseFloat(parts[3].trim());
                return new ActionCommand("custom_swipe", ActionCommand.CommandType.SWIPE,
                        CommandParameters.newBuilder()
                                .startPosition(x1, y1)
                                .endPosition(x2, y2)
                                .duration(300L)
                                .build());
            }
        } else if (commandText.startsWith("type(") && commandText.endsWith(")")) {
            String text = commandText.substring(5, commandText.length() - 1);
            return new ActionCommand("custom_type", ActionCommand.CommandType.TYPE_TEXT,
                    CommandParameters.newBuilder().text(text).build());
        }
        return null;
    }

    private void addLogEntry(String message) {
        commandLog.add(0, new CommandLogEntry(System.currentTimeMillis(), message));
        commandLogAdapter.notifyItemInserted(0);
        commandLogRecyclerView.scrollToPosition(0);
    }

    private void updateStatusText(String status) {
        statusTextView.setText("Status: " + status);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_permissions) {
            checkPermissions();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        Toast.makeText(this, "GENOS Overlay System v1.0.0\nInput Emulator & Command Executor", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == ACCESSIBILITY_PERMISSION_REQUEST) {
            checkPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
        
        // Bind to accessibility service if enabled
        if (accessibilitySwitch.isChecked()) {
            bindToAccessibilityService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unbind from accessibility service to avoid memory leaks
        unbindFromAccessibilityService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (overlayService != null) {
            overlayService.cleanup();
        }
        
        if (commandExecutor != null) {
            commandExecutor.shutdown();
        }
    }

    // Service connection for accessibility service
    private ServiceConnection accessibilityServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Accessibility service connected");
            InputEmulatorService.LocalBinder binder = (InputEmulatorService.LocalBinder) service;
            inputEmulatorService = binder.getService();
            isAccessibilityServiceConnected = true;
            addLogEntry("Accessibility service connected");
            updateStatusText("Accessibility service ready");
            
            // Connect command executor to input emulator
            if (commandExecutor != null) {
                commandExecutor.setInputEmulatorService(inputEmulatorService);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Accessibility service disconnected");
            inputEmulatorService = null;
            isAccessibilityServiceConnected = false;
            addLogEntry("Accessibility service disconnected");
            updateStatusText("Accessibility service disconnected");
        }
    };

    private void bindToAccessibilityService() {
        if (!isAccessibilityServiceConnected) {
            Intent serviceIntent = new Intent(this, InputEmulatorService.class);
            bindService(serviceIntent, accessibilityServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindFromAccessibilityService() {
        if (isAccessibilityServiceConnected) {
            unbindService(accessibilityServiceConnection);
            isAccessibilityServiceConnected = false;
        }
    }

    // Command log entry class
    private static class CommandLogEntry {
        long timestamp;
        String message;

        CommandLogEntry(long timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }
    }

    // Simple adapter for command log
    private class CommandLogAdapter extends RecyclerView.Adapter<CommandLogAdapter.ViewHolder> {
        private List<CommandLogEntry> logEntries;

        CommandLogAdapter(List<CommandLogEntry> logEntries) {
            this.logEntries = logEntries;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            CommandLogEntry entry = logEntries.get(position);
            holder.textView.setText(String.format("%s", entry.message));
        }

        @Override
        public int getItemCount() {
            return logEntries.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}