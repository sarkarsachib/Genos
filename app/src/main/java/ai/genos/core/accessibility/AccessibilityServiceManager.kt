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
     * Stores the given app transition in the manager's transition history and notifies registered listeners.
     *
     * The transition history is capped at MAX_TRANSITION_HISTORY; if the cap is exceeded, oldest entries are removed.
     *
     * @param transition The app transition event to record and broadcast to listeners.
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
     * Retrieve the most recent recorded app transitions up to a specified limit.
     *
     * @param limit Maximum number of transitions to return; if fewer transitions are stored the full history is returned.
     * @return A list of up to `limit` most recent `AppTransition` objects, ordered from oldest to newest among the returned items.
     */
    fun getRecentTransitions(limit: Int = 10): List<AppTransition> {
        return transitionLock.read {
            transitions.takeLast(limit.coerceAtMost(transitions.size))
        }
    }
    
    /**
     * Retrieve a snapshot of all stored app transitions.
     *
     * @return A list containing a copy of every stored AppTransition, in insertion order (oldest first).
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
     * Sets the service running state and updates the heartbeat timestamp.
     *
     * Updates the internal running flag, records the current time as the last heartbeat, and notifies registered listeners of the state change.
     *
     * @param isRunning true if the accessibility service is running, false otherwise.
     */
    fun updateServiceState(isRunning: Boolean) {
        isServiceRunning = isRunning
        lastHeartbeat = System.currentTimeMillis()
        
        Logger.logDebug(TAG, "Service state updated: $isRunning")
        notifyStateChangeListeners(isRunning)
    }
    
    /**
     * Determines whether the accessibility service is currently considered active.
     *
     * Considers the service active only when the internal running flag is set and the last heartbeat was within 30 seconds.
     *
     * @return `true` if the running flag is set and the most recent heartbeat is within 30 seconds, `false` otherwise.
     */
    fun isServiceRunning(): Boolean {
        return isServiceRunning && (System.currentTimeMillis() - lastHeartbeat) < 30000 // 30 second timeout
    }
    
    /**
     * Constructs a snapshot of current service statistics.
     *
     * @return A ServiceStatistics instance containing the total number of recorded transitions, whether the service is considered running, the last heartbeat timestamp, and the current system time. 
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
     * Registers a listener to receive transition and service state change notifications.
     *
     * The listener is retained until explicitly removed and will be invoked when a new transition is recorded or the service state changes.
     *
     * @param listener Listener that will receive `onTransition` and `onStateChanged` callbacks.
     */
    fun addServiceStateListener(listener: ServiceStateListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }
    
    /**
     * Unregisters a ServiceStateListener so it no longer receives transition or state-change notifications.
     *
     * @param listener The listener to remove from the manager's listener list.
     */
    fun removeServiceStateListener(listener: ServiceStateListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }
    
    /**
     * Notifies all registered listeners of a new app transition.
     *
     * Invokes `onTransition(transition)` on each listener while holding the listeners lock; catches and logs any exception thrown by a listener so other listeners still receive the notification.
     *
     * @param transition The `AppTransition` to deliver to listeners.
     */
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
    
    /**
     * Notify registered listeners about a change in the accessibility service running state.
     *
     * Exceptions thrown by individual listeners are caught and logged so notification continues for remaining listeners.
     *
     * @param isRunning `true` if the service is now running, `false` if it is stopped.
     */
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
    /**
 * Invoked when a new application transition is recorded.
 *
 * @param transition The recorded app transition event.
 */
fun onTransition(transition: AppTransition)
    /**
 * Notifies the listener that the accessibility service running state has changed.
 *
 * @param isRunning `true` if the service is now running, `false` otherwise.
 */
fun onStateChanged(isRunning: Boolean)
}

// Service statistics data class
data class ServiceStatistics(
    val totalTransitions: Int,
    val isServiceRunning: Boolean,
    val lastHeartbeat: Long,
    val currentTime: Long
)