package com.example.androidproject.overlay.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
/**
 * Renders an overlay card that displays GenOS status, control buttons, and optional sections
 * for the last action, UI tree, and OCR results based on the provided view model's state.
 *
 * The composable observes `viewModel.state` and updates the visible sections and button actions
 * in response to changes.
 *
 * @param viewModel Provides the UI state and action handlers used by the overlay.
 */
fun OverlayView(viewModel: OverlayViewModel) {
    val state by viewModel.state.collectAsState()
    
    Card(
        modifier = Modifier
            .width(280.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Status Header
            StatusHeader(
                genosStatus = state.genosStatus,
                currentApp = state.currentApp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Control Buttons
            ControlButtons(
                isAutomationRunning = state.isAutomationRunning,
                onStartAutomation = { viewModel.startMonitoring() },
                onStopAutomation = { viewModel.stopMonitoring() },
                onRequestOcr = { viewModel.requestOCR() },
                onToggleUiTree = { viewModel.toggleUiTreeVisibility() }
            )
            
            // Last Action Display
            AnimatedVisibility(
                visible = state.lastAction.isNotEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                LastActionDisplay(action = state.lastAction)
            }
            
            // UI Tree Display
            AnimatedVisibility(
                visible = state.showUiTree && state.uiTree.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                UiTreeDisplay(tree = state.uiTree)
            }
            
            // OCR Result Display
            AnimatedVisibility(
                visible = state.ocrText.isNotEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                OcrResultDisplay(text = state.ocrText)
            }
        }
    }
}

/**
 * Displays a two-line status header showing the GenOS status and the current app.
 *
 * The status line is color-coded based on the status text: `Running` → green, `Executing` → yellow,
 * `Error` → red, otherwise the default on-surface color. The current app name is shown below with
 * smaller styling and truncated with an ellipsis if it overflows.
 *
 * @param genosStatus The status text to display (used to determine the status color).
 * @param currentApp The name of the currently focused application to display beneath the status.
 */
@Composable
private fun StatusHeader(genosStatus: String, currentApp: String) {
    Column {
        Text(
            text = genosStatus,
            style = MaterialTheme.typography.titleMedium,
            color = when {
                genosStatus.contains("Running", ignoreCase = true) -> Color.Green
                genosStatus.contains("Executing", ignoreCase = true) -> Color.Yellow
                genosStatus.contains("Error", ignoreCase = true) -> Color.Red
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = currentApp,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Displays a horizontal row of three evenly spaced control buttons for automation, OCR, and UI-tree toggle.
 *
 * The primary button toggles between "Start" and "Stop" and changes its background color based on `isAutomationRunning`.
 *
 * @param isAutomationRunning Whether automation is currently running; controls the primary button's label and color.
 * @param onStartAutomation Invoked when the primary button is pressed while automation is not running.
 * @param onStopAutomation Invoked when the primary button is pressed while automation is running.
 * @param onRequestOcr Invoked when the "OCR" button is pressed.
 * @param onToggleUiTree Invoked when the "Tree" button is pressed.
 */
@Composable
private fun ControlButtons(
    isAutomationRunning: Boolean,
    onStartAutomation: () -> Unit,
    onStopAutomation: () -> Unit,
    onRequestOcr: () -> Unit,
    onToggleUiTree: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = if (isAutomationRunning) onStopAutomation else onStartAutomation,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAutomationRunning) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Text(
                text = if (isAutomationRunning) "Stop" else "Start",
                fontSize = 12.sp
            )
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Button(
            onClick = onRequestOcr,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(text = "OCR", fontSize = 12.sp)
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Button(
            onClick = onToggleUiTree,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text(text = "Tree", fontSize = 12.sp)
        }
    }
}

/**
 * Displays the most recent action text inside a rounded, full-width surface.
 *
 * @param action The last action description to display. */
@Composable
private fun LastActionDisplay(action: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = action,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 11.sp
        )
    }
}

/**
 * Displays a titled, scrollable surface showing the provided UI tree text.
 *
 * The content is presented with theme-aware colors and typography and constrained vertically
 * so the tree text can be scrolled when it exceeds the visible area.
 *
 * @param tree The UI hierarchy text to display inside the scrollable area.
 */
@Composable
private fun UiTreeDisplay(tree: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .heightIn(max = 150.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column {
            Text(
                text = "UI Tree:",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            
            Text(
                text = tree,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 12.sp
            )
        }
    }
}

/**
 * Provides a ScrollState usable as a scrollbar adapter on Compose versions that do not expose a dedicated adapter.
 *
 * @return a `ScrollState` instance suitable for driving scrollbar position and observing scroll offset.
 */
@Composable
private fun rememberScrollbarAdapter() = androidx.compose.foundation.rememberScrollState()

/**
 * Displays OCR output inside a rounded, themed surface with a header and scrollable content.
 *
 * Shows a fixed header "OCR Result:" followed by the provided text constrained to a maximum
 * height and scrollable when content exceeds the space.
 *
 * @param text The OCR-recognized text to display.
 */
@Composable
private fun OcrResultDisplay(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column {
            Text(
                text = "OCR Result:",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f))
            
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .heightIn(max = 80.dp)
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 11.sp
            )
        }
    }
}