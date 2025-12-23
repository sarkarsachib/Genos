package ai.genos.core.accessibility

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import java.util.Deque

/**
 * Service manager for handling global accessibility service state and transitions
 */
class AccessibilityServiceManager {
    
    companion object {
        private const val TAG = "AccessibilityServiceManager"
        private const val MAX_TRANSITION_HISTORY = 100
    }
    
    private val transitions = ConcurrentLinkedQueue<AppTransition>()
    private val transitionLock = ReentrantReadWriteLock()
    private val listeners = mutableListOf<ServiceStateListener>()
    private var isServiceRunning = false
    private var lastHeartbeat = 0L
    
    /**
     * Record an app transition
     */
    fun recordTransition(transition: AppTransition) {
        transitionLock.write {
            transitions.offer(transition)
            
            // Maintain maximum size
            while (transitions.size > MAX_TRANSITION_HISTORY) {
                transitions.poll()
            }
        }
        
        Logger.logDebug(TAG, "Recorded transition: $transition")
        notifyTransitionListeners(transition)
    }
    
    /**
     * Get recent transitions
     */
    fun getRecentTransitions(limit: Int = 10): List<AppTransition> {
        return transitionLock.read {
            transitions.takeLast(limit.coerceAtMost(transitions.size))
        }
    }
    
    /**
     * Get all transitions
     */
    fun getAllTransitions(): List<AppTransition> {
        return transitionLock.read {
            transitions.toList()
        }
    }
    
    /**
     * Clear transition history
     */
    fun clearTransitions() {
        transitionLock.write {
            transitions.clear()
        }
    }
    
    /**
     * Update service running state
     */
    fun updateServiceState(isRunning: Boolean) {
        isServiceRunning = isRunning
        lastHeartbeat = System.currentTimeMillis()
        
        Logger.logDebug(TAG, "Service state updated: $isRunning")
        notifyStateChangeListeners(isRunning)
    }
    
    /**
     * Check if service is running
     */
    fun isServiceRunning(): Boolean {
        return isServiceRunning && (System.currentTimeMillis() - lastHeartbeat) < 30000 // 30 second timeout
    }
    
    /**
     * Get service statistics
     */
    fun getServiceStatistics(): ServiceStatistics {
        return transitionLock.read {
            ServiceStatistics(
                totalTransitions = transitions.size,
                isServiceRunning = isServiceRunning,
                lastHeartbeat = lastHeartbeat,
                currentTime = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Add service state listener
     */
    fun addServiceStateListener(listener: ServiceStateListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }
    
    /**
     * Remove service state listener
     */
    fun removeServiceStateListener(listener: ServiceStateListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }
    
    private fun notifyTransitionListeners(transition: AppTransition) {
        synchronized(listeners) {
            listeners.forEach { listener ->
                try {
                    listener.onTransition(transition)
                } catch (e: Exception) {
                    Logger.logError(TAG, "Error notifying transition listener", e)
                }
            }
        }
    }
    
    private fun notifyStateChangeListeners(isRunning: Boolean) {
        synchronized(listeners) {
            listeners.forEach { listener ->
                try {
                    listener.onStateChanged(isRunning)
                } catch (e: Exception) {
                    Logger.logError(TAG, "Error notifying state change listener", e)
                }
            }
        }
    }
}

// Service state listener interface
interface ServiceStateListener {
    fun onTransition(transition: AppTransition)
    fun onStateChanged(isRunning: Boolean)
}

// Service statistics data class
data class ServiceStatistics(
    val totalTransitions: Int,
    val isServiceRunning: Boolean,
    val lastHeartbeat: Long,
    val currentTime: Long
)