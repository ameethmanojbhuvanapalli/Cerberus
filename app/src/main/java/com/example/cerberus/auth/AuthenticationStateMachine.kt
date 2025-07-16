package com.example.cerberus.auth

import android.util.Log
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Deterministic Finite Automaton (DFA) for predictable authentication flow
 * 
 * States:
 * - UNAUTHENTICATED: Initial state, user needs authentication
 * - PROMPTING: Authentication prompt is active
 * - AUTHENTICATED: User successfully authenticated
 * 
 * State Transitions:
 * - User opens locked app → prompt should come (UNAUTHENTICATED → PROMPTING)
 * - User provides correct auth → app opens (PROMPTING → AUTHENTICATED)
 * - User destroys prompt without auth → auth failure (PROMPTING → UNAUTHENTICATED)
 * - User exits app (true app switch) → timeout set (AUTHENTICATED → UNAUTHENTICATED)
 */
class AuthenticationStateMachine {
    
    enum class State {
        UNAUTHENTICATED,
        PROMPTING,
        AUTHENTICATED
    }
    
    sealed class Event {
        data class PromptRequested(val packageName: String) : Event()
        data class AuthenticationSucceeded(val packageName: String) : Event()
        data class AuthenticationFailed(val packageName: String) : Event()
        data class PromptDestroyed(val packageName: String) : Event()
        data class AppExited(val packageName: String) : Event()
    }
    
    data class AuthState(
        val state: State,
        val authenticatedTime: Long = 0L,
        val promptActive: Boolean = false
    )
    
    private val TAG = "AuthStateMachine"
    
    // Thread-safe state storage per package
    private val packageStates = ConcurrentHashMap<String, AtomicReference<AuthState>>()
    
    // Listeners for state changes
    private val stateChangeListeners = mutableListOf<(String, State, State) -> Unit>()
    
    /**
     * Get current state for a package
     */
    fun getCurrentState(packageName: String): State {
        return getAuthState(packageName).state
    }
    
    /**
     * Check if authentication is valid for a package (considering timeout)
     */
    fun isAuthenticated(packageName: String, timeoutMs: Long): Boolean {
        val authState = getAuthState(packageName)
        return when (authState.state) {
            State.AUTHENTICATED -> {
                if (authState.authenticatedTime == Long.MAX_VALUE) {
                    true // No timeout set
                } else {
                    System.currentTimeMillis() <= authState.authenticatedTime
                }
            }
            else -> false
        }
    }
    
    /**
     * Check if prompt is currently active for a package
     */
    fun isPromptActive(packageName: String): Boolean {
        return getAuthState(packageName).promptActive
    }
    
    /**
     * Process an event and return whether state transition occurred
     */
    fun processEvent(event: Event): Boolean {
        val packageName = when (event) {
            is Event.PromptRequested -> event.packageName
            is Event.AuthenticationSucceeded -> event.packageName
            is Event.AuthenticationFailed -> event.packageName
            is Event.PromptDestroyed -> event.packageName
            is Event.AppExited -> event.packageName
        }
        
        val currentState = getAuthState(packageName)
        val newState = when (event) {
            is Event.PromptRequested -> handlePromptRequested(currentState, event)
            is Event.AuthenticationSucceeded -> handleAuthenticationSucceeded(currentState, event)
            is Event.AuthenticationFailed -> handleAuthenticationFailed(currentState, event)
            is Event.PromptDestroyed -> handlePromptDestroyed(currentState, event)
            is Event.AppExited -> handleAppExited(currentState, event)
        }
        
        if (newState != currentState) {
            setState(packageName, newState)
            notifyStateChange(packageName, currentState.state, newState.state)
            Log.d(TAG, "State transition for $packageName: ${currentState.state} → ${newState.state}")
            return true
        }
        
        return false
    }
    
    /**
     * Set timeout for authenticated app (called on app exit)
     */
    fun setAuthenticationTimeout(packageName: String, timeoutMs: Long) {
        val currentState = getAuthState(packageName)
        if (currentState.state == State.AUTHENTICATED) {
            val newExpiration = System.currentTimeMillis() + timeoutMs
            val newState = currentState.copy(authenticatedTime = newExpiration)
            setState(packageName, newState)
            Log.d(TAG, "Set timeout for $packageName until $newExpiration")
        }
    }
    
    /**
     * Clear all authentication states
     */
    fun clearAllStates() {
        packageStates.clear()
        Log.d(TAG, "Cleared all authentication states")
    }
    
    /**
     * Register listener for state changes
     */
    fun addStateChangeListener(listener: (String, State, State) -> Unit) {
        stateChangeListeners.add(listener)
    }
    
    /**
     * Remove state change listener
     */
    fun removeStateChangeListener(listener: (String, State, State) -> Unit) {
        stateChangeListeners.remove(listener)
    }
    
    // Private helper methods
    
    private fun getAuthState(packageName: String): AuthState {
        return packageStates.getOrPut(packageName) { 
            AtomicReference(AuthState(State.UNAUTHENTICATED))
        }.get()
    }
    
    private fun setState(packageName: String, newState: AuthState) {
        packageStates.getOrPut(packageName) { 
            AtomicReference(AuthState(State.UNAUTHENTICATED))
        }.set(newState)
    }
    
    private fun notifyStateChange(packageName: String, oldState: State, newState: State) {
        stateChangeListeners.forEach { listener ->
            try {
                listener(packageName, oldState, newState)
            } catch (e: Exception) {
                Log.e(TAG, "Error in state change listener", e)
            }
        }
    }
    
    private fun handlePromptRequested(currentState: AuthState, event: Event.PromptRequested): AuthState {
        return when (currentState.state) {
            State.UNAUTHENTICATED -> {
                // Transition to PROMPTING
                currentState.copy(state = State.PROMPTING, promptActive = true)
            }
            State.PROMPTING -> {
                // Already prompting - no state change but ensure prompt is marked active
                if (!currentState.promptActive) {
                    currentState.copy(promptActive = true)
                } else {
                    currentState
                }
            }
            State.AUTHENTICATED -> {
                // Should not happen in normal flow - but handle gracefully
                Log.w(TAG, "Prompt requested while authenticated for ${event.packageName}")
                currentState
            }
        }
    }
    
    private fun handleAuthenticationSucceeded(currentState: AuthState, event: Event.AuthenticationSucceeded): AuthState {
        return when (currentState.state) {
            State.PROMPTING -> {
                // Transition to AUTHENTICATED
                currentState.copy(
                    state = State.AUTHENTICATED,
                    authenticatedTime = Long.MAX_VALUE, // No timeout until app exit
                    promptActive = false
                )
            }
            State.UNAUTHENTICATED, State.AUTHENTICATED -> {
                // Update authentication time but keep current state
                currentState.copy(
                    state = State.AUTHENTICATED,
                    authenticatedTime = Long.MAX_VALUE,
                    promptActive = false
                )
            }
        }
    }
    
    private fun handleAuthenticationFailed(currentState: AuthState, event: Event.AuthenticationFailed): AuthState {
        return when (currentState.state) {
            State.PROMPTING -> {
                // Stay in PROMPTING state - prompt should handle retries internally
                // Only transition to UNAUTHENTICATED when prompt is destroyed
                currentState
            }
            else -> currentState // No change for other states
        }
    }
    
    private fun handlePromptDestroyed(currentState: AuthState, event: Event.PromptDestroyed): AuthState {
        return when (currentState.state) {
            State.PROMPTING -> {
                // Transition to UNAUTHENTICATED
                currentState.copy(state = State.UNAUTHENTICATED, promptActive = false)
            }
            else -> {
                // Just mark prompt as inactive
                currentState.copy(promptActive = false)
            }
        }
    }
    
    private fun handleAppExited(currentState: AuthState, event: Event.AppExited): AuthState {
        return when (currentState.state) {
            State.AUTHENTICATED -> {
                // Transition to UNAUTHENTICATED - timeout will be set separately
                currentState.copy(state = State.UNAUTHENTICATED)
            }
            else -> currentState // No change for other states
        }
    }
}