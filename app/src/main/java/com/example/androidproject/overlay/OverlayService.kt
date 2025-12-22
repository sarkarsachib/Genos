package com.example.androidproject.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.androidproject.overlay.ui.OverlayView
import com.example.androidproject.overlay.ui.OverlayViewModel
import com.example.androidproject.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var gestureOverlay: View
    
    private val viewModel = OverlayViewModel()
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    private val _isOverlayVisible = MutableStateFlow(true)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Setup overlay window parameters
        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 100
        }
        
        // Create gesture overlay (full screen for touch visualization)
        val gestureParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        
        // Create overlay view with Compose
        overlayView = ComposeView(this).apply {
            setContent {
                val isVisible by isOverlayVisible.collectAsState()
                if (isVisible) {
                    OverlayView(viewModel = viewModel)
                }
            }
            
            // Setup for Compose
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
        }
        
        // Create gesture overlay for touch visualization
        gestureOverlay = LayoutInflater.from(this).inflate(R.layout.gesture_overlay, null)
        
        // Add views to window manager
        windowManager.addView(gestureOverlay, gestureParams)
        windowManager.addView(overlayView, overlayParams)
        
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        
        intent?.let {
            when (it.action) {
                ACTION_START_MONITORING -> {
                    viewModel.startMonitoring()
                }
                ACTION_STOP_MONITORING -> {
                    viewModel.stopMonitoring()
                }
                ACTION_REQUEST_OCR -> {
                    viewModel.requestOCR()
                }
                ACTION_TOGGLE_VISIBILITY -> {
                    _isOverlayVisible.value = !_isOverlayVisible.value
                }
                ACTION_UPDATE_STATUS -> {
                    it.getStringExtra(EXTRA_STATUS)?.let { status ->
                        viewModel.updateStatus(status)
                    }
                }
                ACTION_UPDATE_UI_TREE -> {
                    it.getStringExtra(EXTRA_UI_TREE)?.let { tree ->
                        viewModel.updateUiTree(tree)
                    }
                }
                ACTION_SHOW_TOUCH -> {
                    val x = it.getFloatExtra(EXTRA_X, 0f)
                    val y = it.getFloatExtra(EXTRA_Y, 0f)
                    viewModel.showTouchAt(x, y)
                }
            }
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        
        try {
            windowManager.removeView(overlayView)
            windowManager.removeView(gestureOverlay)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        viewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun getLifecycle(): Lifecycle = lifecycleRegistry
    override fun getViewModelStore(): ViewModelStore = viewModelStore
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    companion object {
        const val ACTION_START_MONITORING = "com.example.androidproject.overlay.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.androidproject.overlay.STOP_MONITORING"
        const val ACTION_REQUEST_OCR = "com.example.androidproject.overlay.REQUEST_OCR"
        const val ACTION_TOGGLE_VISIBILITY = "com.example.androidproject.overlay.TOGGLE_VISIBILITY"
        const val ACTION_UPDATE_STATUS = "com.example.androidproject.overlay.UPDATE_STATUS"
        const val ACTION_UPDATE_UI_TREE = "com.example.androidproject.overlay.UPDATE_UI_TREE"
        const val ACTION_SHOW_TOUCH = "com.example.androidproject.overlay.SHOW_TOUCH"

        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_UI_TREE = "extra_ui_tree"
        const val EXTRA_X = "extra_x"
        const val EXTRA_Y = "extra_y"
    }
}