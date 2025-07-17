package com.example.cerberus.statemachine

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Deterministic Finite Automaton (DFA) based state machine for app lock management.
 * This class manages state transitions for individual apps based on events from
 * the EventProcessor. It maintains a transition table and ensures only valid
 * transitions occur.
 */
class AppStateMachine {
    private val TAG = "AppStateMachine"
    
    // State tracking for each app
    private val appStates = ConcurrentHashMap<String, AppState>()
    
    // Listeners for state changes
    private val listeners = mutableListOf<StateTransitionListener>()
    
    // DFA Transition Table
    // Map of (CurrentState, Event) -> NewState
    private val transitionTable = mapOf(
        // From IDLE
        (AppState.IDLE to AppEvent.APP_LAUNCHED) to AppState.FOREGROUND,
        
        // From FOREGROUND
        (AppState.FOREGROUND to AppEvent.AUTH_PROMPT_SHOWN) to AppState.AUTHENTICATING,
        (AppState.FOREGROUND to AppEvent.BACKGROUND_GESTURE) to AppState.BACKGROUND,
        (AppState.FOREGROUND to AppEvent.APP_CLOSED) to AppState.EXITED,
        
        // From AUTHENTICATING
        (AppState.AUTHENTICATING to AppEvent.AUTH_SUCCESS) to AppState.AUTHENTICATED,
        (AppState.AUTHENTICATING to AppEvent.AUTH_FAILED) to AppState.FOREGROUND,
        (AppState.AUTHENTICATING to AppEvent.BACKGROUND_GESTURE) to AppState.BACKGROUND,
        (AppState.AUTHENTICATING to AppEvent.APP_CLOSED) to AppState.EXITED,
        
        // From AUTHENTICATED
        (AppState.AUTHENTICATED to AppEvent.BACKGROUND_GESTURE) to AppState.BACKGROUND,
        (AppState.AUTHENTICATED to AppEvent.APP_CLOSED) to AppState.EXITED,
        (AppState.AUTHENTICATED to AppEvent.TIMEOUT_EXPIRED) to AppState.EXITED,
        
        // From BACKGROUND
        (AppState.BACKGROUND to AppEvent.APP_RETURNED) to AppState.FOREGROUND,
        (AppState.BACKGROUND to AppEvent.TIMEOUT_EXPIRED) to AppState.EXITED,
        (AppState.BACKGROUND to AppEvent.APP_CLOSED) to AppState.EXITED,
        
        // From EXITED
        (AppState.EXITED to AppEvent.APP_LAUNCHED) to AppState.FOREGROUND
    )
    
    /**
     * Register a listener for state transition events
     */
    @Synchronized
    fun addStateTransitionListener(listener: StateTransitionListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
            Log.d(TAG, "Added state transition listener: ${listener::class.java.simpleName}")
        }
    }
    
    /**
     * Unregister a state transition listener
     */
    @Synchronized
    fun removeStateTransitionListener(listener: StateTransitionListener) {
        listeners.remove(listener)
        Log.d(TAG, "Removed state transition listener: ${listener::class.java.simpleName}")
    }
    
    /**
     * Get the current state of an app
     */
    fun getCurrentState(packageName: String): AppState {
        return appStates[packageName] ?: AppState.IDLE
    }
    
    /**
     * Process an event and potentially transition to a new state
     * 
     * @param packageName The package name of the app
     * @param event The event to process
     * @return true if state transition occurred, false otherwise
     */
    @Synchronized
    fun processEvent(packageName: String, event: AppEvent): Boolean {
        val currentState = getCurrentState(packageName)
        val transitionKey = currentState to event
        
        val newState = transitionTable[transitionKey]
        
        if (newState != null) {
            // Valid transition found
            Log.d(TAG, "State transition for $packageName: $currentState -> $newState (event: $event)")
            
            val oldState = currentState
            appStates[packageName] = newState
            
            // Notify listeners
            notifyStateChanged(packageName, oldState, newState, event)
            
            // Clean up if app exited
            if (newState == AppState.EXITED) {
                appStates.remove(packageName)
                Log.d(TAG, "Cleaned up state for exited app: $packageName")
            }
            
            return true
        } else {
            // Invalid transition
            Log.w(TAG, "Invalid transition for $packageName: $currentState + $event (no valid transition)")
            notifyTransitionRejected(packageName, currentState, event)
            return false
        }
    }
    
    /**
     * Force set state for an app (use with caution)
     */
    @Synchronized
    fun forceSetState(packageName: String, state: AppState) {
        val oldState = getCurrentState(packageName)
        appStates[packageName] = state
        Log.d(TAG, "Force set state for $packageName: $oldState -> $state")
        
        if (state == AppState.EXITED) {
            appStates.remove(packageName)
        }
    }
    
    /**
     * Clear all app states (for cleanup)
     */
    @Synchronized
    fun clearAllStates() {
        appStates.clear()
        Log.d(TAG, "Cleared all app states")
    }
    
    /**
     * Get all currently tracked apps and their states
     */
    fun getAllAppStates(): Map<String, AppState> {
        return appStates.toMap()
    }
    
    /**
     * Check if a transition is valid without executing it
     */
    fun isValidTransition(currentState: AppState, event: AppEvent): Boolean {
        return transitionTable.containsKey(currentState to event)
    }
    
    private fun notifyStateChanged(
        packageName: String,
        fromState: AppState,
        toState: AppState,
        event: AppEvent
    ) {
        listeners.forEach { listener ->
            try {
                listener.onStateChanged(packageName, fromState, toState, event)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying state transition listener", e)
            }
        }
    }
    
    private fun notifyTransitionRejected(
        packageName: String,
        currentState: AppState,
        event: AppEvent
    ) {
        listeners.forEach { listener ->
            try {
                listener.onTransitionRejected(packageName, currentState, event)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying transition rejection to listener", e)
            }
        }
    }
}