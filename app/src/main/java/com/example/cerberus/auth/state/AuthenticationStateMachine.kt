package com.example.cerberus.auth.state

import android.util.Log
import com.example.cerberus.auth.listeners.AuthStateListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Pure state machine for authentication states.
 * Thread-safe and focused only on state transitions and validation.
 */
class AuthenticationStateMachine {
    private val states = ConcurrentHashMap<String, AuthenticationState>()
    private val listeners = mutableListOf<AuthStateListener>()
    private val lock = ReentrantReadWriteLock()
    
    companion object {
        private const val TAG = "AuthStateMachine"
    }

    /**
     * Get the current state for a package
     */
    fun getState(packageName: String): AuthenticationState {
        return lock.read {
            states[packageName] ?: AuthenticationState(packageName, AuthState.UNAUTHENTICATED)
        }
    }

    /**
     * Transition to a new state with validation
     */
    fun transitionTo(packageName: String, newState: AuthState, expirationTime: Long? = null): AuthenticationState {
        return lock.write {
            val currentState = states[packageName] ?: AuthenticationState(packageName, AuthState.UNAUTHENTICATED)
            
            if (!isValidTransition(currentState.state, newState)) {
                Log.w(TAG, "Invalid state transition for $packageName: ${currentState.state} → $newState")
                return currentState
            }

            val newAuthState = AuthenticationState(
                packageName = packageName,
                state = newState,
                expirationTime = expirationTime
            )
            
            states[packageName] = newAuthState
            
            Log.d(TAG, "State machine transition for $packageName: ${currentState.state} → $newState")
            
            // Notify listeners outside the lock
            notifyListeners(currentState, newAuthState)
            
            newAuthState
        }
    }

    /**
     * Set expiration time for an authenticated app
     */
    fun setExpiration(packageName: String, expirationTime: Long): AuthenticationState? {
        return lock.write {
            val currentState = states[packageName]
            if (currentState?.state == AuthState.AUTHENTICATED) {
                val updatedState = currentState.copy(expirationTime = expirationTime)
                states[packageName] = updatedState
                Log.d(TAG, "Updated expiration for $packageName until $expirationTime")
                updatedState
            } else {
                Log.w(TAG, "Cannot set expiration for $packageName - not authenticated")
                null
            }
        }
    }

    /**
     * Clear authentication state for a package
     */
    fun clearState(packageName: String): AuthenticationState? {
        return lock.write {
            val removedState = states.remove(packageName)
            if (removedState != null) {
                val unauthenticatedState = AuthenticationState(packageName, AuthState.UNAUTHENTICATED)
                notifyListeners(removedState, unauthenticatedState)
                Log.d(TAG, "Cleared state for $packageName")
            }
            removedState
        }
    }

    /**
     * Clear all authentication states
     */
    fun clearAllStates() {
        lock.write {
            val clearedStates = states.toMap()
            states.clear()
            Log.d(TAG, "Cleared all authentication states")
            
            // Notify listeners for each cleared state
            clearedStates.values.forEach { oldState ->
                val unauthenticatedState = AuthenticationState(oldState.packageName, AuthState.UNAUTHENTICATED)
                notifyListeners(oldState, unauthenticatedState)
            }
        }
    }

    /**
     * Get all current states (for debugging/monitoring)
     */
    fun getAllStates(): Map<String, AuthenticationState> {
        return lock.read {
            states.toMap()
        }
    }

    /**
     * Register a state change listener
     */
    fun registerListener(listener: AuthStateListener) {
        lock.write {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
                Log.d(TAG, "Registered auth state listener: ${listener::class.java.simpleName}")
            }
        }
    }

    /**
     * Unregister a state change listener
     */
    fun unregisterListener(listener: AuthStateListener) {
        lock.write {
            listeners.remove(listener)
            Log.d(TAG, "Unregistered auth state listener: ${listener::class.java.simpleName}")
        }
    }

    /**
     * Validate if a state transition is allowed
     */
    private fun isValidTransition(from: AuthState, to: AuthState): Boolean {
        return when (from) {
            AuthState.UNAUTHENTICATED -> to == AuthState.PROMPTING
            AuthState.PROMPTING -> to == AuthState.AUTHENTICATED || to == AuthState.UNAUTHENTICATED
            AuthState.AUTHENTICATED -> to == AuthState.UNAUTHENTICATED || to == AuthState.PROMPTING
        }
    }

    /**
     * Notify listeners of state changes
     */
    private fun notifyListeners(oldState: AuthenticationState, newState: AuthenticationState) {
        // Create a copy of listeners to avoid concurrent modification
        val currentListeners = listeners.toList()
        currentListeners.forEach { listener ->
            try {
                listener.onStateChanged(oldState, newState)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying auth state listener", e)
            }
        }
    }
}