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
    
    fun updateStatus(status: String) {
        viewModelScope.launch {
            _state.update { it.copy(genosStatus = "GENOS: $status") }
        }
    }
    
    fun updateAppContext(packageName: String, appName: String) {
        viewModelScope.launch {
            _state.update { it.copy(currentApp = "$appName ($packageName)") }
        }
    }
    
    fun startMonitoring() {
        viewModelScope.launch {
            _state.update { it.copy(
                isAutomationRunning = true,
                genosStatus = "GENOS: Monitoring"
            ) }
            Log.d("OverlayViewModel", "Automation monitoring started")
        }
    }
    
    fun stopMonitoring() {
        viewModelScope.launch {
            _state.update { it.copy(
                isAutomationRunning = false,
                genosStatus = "GENOS: Stopped"
            ) }
            Log.d("OverlayViewModel", "Automation monitoring stopped")
        }
    }
    
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
    
    fun toggleUiTreeVisibility() {
        viewModelScope.launch {
            _state.update { it.copy(showUiTree = !it.showUiTree) }
        }
    }
    
    fun requestOCR() {
        viewModelScope.launch {
            _state.update { it.copy(genosStatus = "GENOS: Performing OCR...") }
            Log.d("OverlayViewModel", "OCR requested")
        }
    }
    
    fun updateOcrText(text: String) {
        viewModelScope.launch {
            _state.update { it.copy(
                ocrText = text,
                genosStatus = "GENOS: OCR Complete"
            ) }
            Log.d("OverlayViewModel", "OCR completed: ${text.take(50)}...")
        }
    }
    
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