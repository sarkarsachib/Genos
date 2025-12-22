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
     * Adds an app transition to the manager's history and notifies registered listeners.
     *
     * The transition is appended to the internal history and the history is trimmed to
     * the configured maximum size if necessary. Registered ServiceStateListener instances
     * are notified of the recorded transition.
     *
     * @param transition The app transition to record.
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
     * Retrieve up to the specified number of most recent transitions.
     *
     * @param limit The maximum number of transitions to return (defaults to 10). If `limit` is greater than
     * the available history, all recorded transitions are returned.
     * @return A list of up to `limit` most recent `AppTransition` objects in chronological order (oldest first).
     */
    fun getRecentTransitions(limit: Int = 10): List<AppTransition> {
        return transitionLock.read {
            transitions.takeLast(limit.coerceAtMost(transitions.size))
        }
    }
    
    /**
     * Return a snapshot of the full transition history.
     *
     * The returned list is a copy of the stored transitions at the time of the call.
     *
     * @return A list of all recorded AppTransition objects in chronological order (oldest first).
     */
    fun getAllTransitions(): List<AppTransition> {
        return transitionLock.read {
            transitions.toList()
        }
    }
    
    /**
     * Removes all recorded app transitions from the manager's history.
     *
     * After this call, the transition history is empty until new transitions are recorded.
     */
    fun clearTransitions() {
        transitionLock.write {
            transitions.clear()
        }
    }
    
    /**
     * Set the current service running state and record a heartbeat timestamp.
     *
     * Updates the internal running flag, refreshes the last heartbeat to the current time,
     * and notifies registered listeners of the state change.
     *
     * @param isRunning `true` to mark the service as running, `false` to mark it as stopped.
     */
    fun updateServiceState(isRunning: Boolean) {
        isServiceRunning = isRunning
        lastHeartbeat = System.currentTimeMillis()
        
        Logger.logDebug(TAG, "Service state updated: $isRunning")
        notifyStateChangeListeners(isRunning)
    }
    
    /**
     * Determines whether the accessibility service is currently considered running.
     *
     * @return `true` if the service was marked running and its last heartbeat occurred within the past 30 seconds, `false` otherwise.
     */
    fun isServiceRunning(): Boolean {
        return isServiceRunning && (System.currentTimeMillis() - lastHeartbeat) < 30000 // 30 second timeout
    }
    
    /**
     * Fetches aggregated statistics about the accessibility service and its transition history.
     *
     * @return A ServiceStatistics containing totalTransitions (number of recorded transitions),
     *         isServiceRunning (whether the service is considered running), lastHeartbeat (epoch ms),
     *         and currentTime (current epoch ms).
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
     * Registers a listener to receive notifications about app transitions and service state changes.
     *
     * Registration is thread-safe; the listener will start receiving callbacks for subsequent transitions
     * and state updates.
     *
     * @param listener The ServiceStateListener to register.
     */
    fun addServiceStateListener(listener: ServiceStateListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }
    
    /**
     * Unregisters a service state listener so it will no longer receive transition or state-change notifications.
     *
     * @param listener The listener to remove.
     */
    fun removeServiceStateListener(listener: ServiceStateListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }
    
    /**
     * Notifies all registered service state listeners of a new app transition.
     *
     * Calls `onTransition` on each listener with the provided transition and catches/logs
     * exceptions from individual listeners so notification continues for others.
     *
     * @param transition The `AppTransition` to deliver to each listener.
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
     * Notify all registered ServiceStateListener instances that the service running state has changed.
     *
     * Listener callbacks are invoked under synchronization; exceptions thrown by individual listeners
     * are caught and logged so notification of other listeners continues.
     *
     * @param isRunning `true` when the service is running, `false` otherwise.
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
 * Invoked when an application transition is recorded.
 *
 * @param transition Details of the recorded application transition event.
 */
fun onTransition(transition: AppTransition)
    /**
 * Called when the global accessibility service running state changes.
 *
 * @param isRunning `true` if the service is currently running, `false` otherwise.
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