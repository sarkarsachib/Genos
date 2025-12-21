package com.genos.accessibility.aggregator

import android.util.Log
import com.genos.accessibility.model.UiElement
import com.genos.accessibility.model.UiTree
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScreenStateAggregator {
    
    companion object {
        private const val TAG = "ScreenStateAggregator"
        private const val MAX_HISTORY_SIZE = 10
    }
    
    private val _currentUiTree = MutableStateFlow<UiTree?>(null)
    val currentUiTree: StateFlow<UiTree?> = _currentUiTree.asStateFlow()
    
    private val _currentAppPackage = MutableStateFlow<String?>(null)
    val currentAppPackage: StateFlow<String?> = _currentAppPackage.asStateFlow()
    
    private val _appTransitionEvents = MutableStateFlow<AppTransitionEvent?>(null)
    val appTransitionEvents: StateFlow<AppTransitionEvent?> = _appTransitionEvents.asStateFlow()
    
    private val _uiTreeHistory = MutableStateFlow<List<UiTree>>(emptyList())
    val uiTreeHistory: StateFlow<List<UiTree>> = _uiTreeHistory.asStateFlow()
    
    private var previousPackageName: String? = null
    
    private val jsonEncoder = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    fun onAccessibilityEvent(packageName: String) {
        try {
            if (packageName != _currentAppPackage.value) {
                val previousPackage = _currentAppPackage.value
                _currentAppPackage.value = packageName
                
                val transitionEvent = AppTransitionEvent(
                    previousPackage = previousPackage,
                    newPackage = packageName,
                    timestamp = System.currentTimeMillis()
                )
                
                _appTransitionEvents.value = transitionEvent
                previousPackageName = previousPackage
                
                Log.i(TAG, "App transition detected: ${transitionEvent.previousPackage ?: "NULL"} -> ${transitionEvent.newPackage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling accessibility event: ${e.message}", e)
        }
    }

    fun onUiTreeUpdate(uiTree: UiTree) {
        try {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "UI tree updated for package: ${uiTree.packageName}, elements: ${countElements(uiTree.root)}")
            }
            
            _currentUiTree.value = uiTree
            
            updateHistory(uiTree)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating UI tree: ${e.message}", e)
        }
    }
    
    private fun updateHistory(uiTree: UiTree) {
        val currentHistory = _uiTreeHistory.value.toMutableList()
        currentHistory.add(uiTree)
        
        if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.removeAt(0)
        }
        
        _uiTreeHistory.value = currentHistory
    }

    fun getCurrentUiTreeSnapshot(): UiTree? {
        return _currentUiTree.value
    }

    fun getCurrentPackage(): String? {
        return _currentAppPackage.value
    }

    fun getSerializedUiTree(): String? {
        return try {
            _currentUiTree.value?.let { tree ->
                jsonEncoder.encodeToString(tree)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error serializing UI tree: ${e.message}", e)
            null
        }
    }

    fun getSerializedElement(element: UiElement): String? {
        return try {
            jsonEncoder.encodeToString(element)
        } catch (e: Exception) {
            Log.e(TAG, "Error serializing element: ${e.message}", e)
            null
        }
    }
    
    private fun countElements(element: UiElement): Int {
        return 1 + element.children.sumOf { countElements(it) }
    }

    fun clear() {
        _currentUiTree.value = null
        _currentAppPackage.value = null
        _appTransitionEvents.value = null
        _uiTreeHistory.value = emptyList()
        previousPackageName = null
        Log.i(TAG, "ScreenStateAggregator cleared")
    }
    
    fun getUiTreeHistory(): List<UiTree> {
        return _uiTreeHistory.value
    }
}

data class AppTransitionEvent(
    val previousPackage: String?,
    val newPackage: String,
    val timestamp: Long
)