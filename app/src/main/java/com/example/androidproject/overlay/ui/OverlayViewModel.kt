package com.example.androidproject.overlay.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OverlayViewModel : ViewModel() {
    
    private val _state = MutableStateFlow(OverlayViewState())
    val state: StateFlow<OverlayViewState> = _state.asStateFlow()
    
    /**
     * Updates the displayed GENOS status text in the view state.
     *
     * @param status The status message to append after "GENOS: ".
     */
    fun updateStatus(status: String) {
        viewModelScope.launch {
            _state.update { it.copy(genosStatus = "GENOS: $status") }
        }
    }
    
    /**
     * Updates the ViewModel state to reflect the currently foreground app shown in the overlay.
     *
     * @param packageName The application's package identifier (for example, `com.example.app`).
     * @param appName The application's display name.
     */
    fun updateAppContext(packageName: String, appName: String) {
        viewModelScope.launch {
            _state.update { it.copy(currentApp = "$appName ($packageName)") }
        }
    }
    
    /**
     * Starts automation monitoring and updates the overlay state to reflect that monitoring is active.
     *
     * Sets `isAutomationRunning` to `true` and updates `genosStatus` to "GENOS: Monitoring".
     */
    fun startMonitoring() {
        viewModelScope.launch {
            _state.update { it.copy(
                isAutomationRunning = true,
                genosStatus = "GENOS: Monitoring"
            ) }
            Log.d("OverlayViewModel", "Automation monitoring started")
        }
    }
    
    /**
     * Stops automation monitoring and updates the overlay view state to reflect that monitoring has stopped.
     *
     * Sets `isAutomationRunning` to `false` and `genosStatus` to `"GENOS: Stopped"` in the view state, and logs the stop event.
     */
    fun stopMonitoring() {
        viewModelScope.launch {
            _state.update { it.copy(
                isAutomationRunning = false,
                genosStatus = "GENOS: Stopped"
            ) }
            Log.d("OverlayViewModel", "Automation monitoring stopped")
        }
    }
    
    /**
     * Displays a temporary touch visualization at the given overlay coordinates.
     *
     * The visualization is shown immediately and is cleared after 2 seconds.
     *
     * @param x The x coordinate in pixels relative to the overlay.
     * @param y The y coordinate in pixels relative to the overlay.
     * @param type The kind of touch to visualize (defaults to `TouchType.TAP`).
     */
    fun showTouchAt(x: Float, y: Float, type: TouchType = TouchType.TAP) {
        viewModelScope.launch {
            _state.update { it.copy(
                touchVisualization = TouchVisualization(x = x, y = y, type = type),
                lastAction = "${type.name}: (${x.toInt()}, ${y.toInt()})"
            ) }
            // Clear the visualization after 2 seconds
            kotlinx.coroutines.delay(2000)
            _state.update { it.copy(touchVisualization = null) }
        }
    }
    
    /**
     * Shows the provided UI tree text in the overlay and then hides it after five seconds.
     *
     * Updates the view state to set `uiTree` to the given text and `showUiTree` to `true`, then clears `showUiTree` (sets to `false`) after 5 seconds.
     *
     * @param tree The UI tree text to display in the overlay.
     */
    fun updateUiTree(tree: String) {
        viewModelScope.launch {
            _state.update { it.copy(
                uiTree = tree,
                showUiTree = true
            ) }
            // Hide UI tree after 5 seconds
            kotlinx.coroutines.delay(5000)
            _state.update { it.copy(showUiTree = false) }
        }
    }
    
    /**
     * Toggles the `showUiTree` visibility flag in the view model's state.
     *
     * Flips `showUiTree` between `true` and `false` so observers of `state` see the updated visibility.
     */
    fun toggleUiTreeVisibility() {
        viewModelScope.launch {
            _state.update { it.copy(showUiTree = !it.showUiTree) }
        }
    }
    
    /**
     * Signals that an OCR operation should start and updates the overlay status.
     *
     * Updates the ViewModel state `genosStatus` to "GENOS: Performing OCR..." and emits a debug
     * log indicating an OCR request.
     */
    fun requestOCR() {
        viewModelScope.launch {
            _state.update { it.copy(genosStatus = "GENOS: Performing OCR...") }
            Log.d("OverlayViewModel", "OCR requested")
        }
    }
    
    /**
     * Updates the stored OCR result and sets the visible status to "GENOS: OCR Complete".
     *
     * @param text The OCR-recognized text to store in the view state.
     */
    fun updateOcrText(text: String) {
        viewModelScope.launch {
            _state.update { it.copy(
                ocrText = text,
                genosStatus = "GENOS: OCR Complete"
            ) }
            Log.d("OverlayViewModel", "OCR completed: ${text.take(50)}...")
        }
    }
    
    /**
     * Updates the overlay state to reflect that the given command is being executed and logs the command.
     *
     * @param command The command text to display as the last action and include in logs.
     */
    fun executeCommand(command: String) {
        viewModelScope.launch {
            _state.update { it.copy(
                lastAction = "Executing: $command",
                genosStatus = "GENOS: Executing Command"
            ) }
            Log.d("OverlayViewModel", "Executing command: $command")
        }
    }
}