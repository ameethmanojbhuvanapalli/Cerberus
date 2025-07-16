package com.example.cerberus.auth.manager

import android.content.Context
import android.util.Log
import com.example.cerberus.auth.listeners.AuthStateListener
import com.example.cerberus.auth.state.AuthState
import com.example.cerberus.auth.state.AuthenticationState
import com.example.cerberus.auth.state.AuthenticationStateMachine
import com.example.cerberus.data.IdleTimeoutCache

/**
 * Manages authentication state machine instances and coordinates with business logic.
 * Handles timeout logic and integrates with the authentication service.
 */
class AuthenticationStateManager(private val context: Context) {
    private val stateMachine = AuthenticationStateMachine()
    
    companion object {
        private const val TAG = "AuthStateManager"
    }

    /**
     * Check if an app is currently authenticated (not expired)
     */
    fun isAuthenticated(packageName: String): Boolean {
        val state = stateMachine.getState(packageName)
        return state.isAuthenticated()
    }

    /**
     * Start authentication prompting for an app
     */
    fun startPrompting(packageName: String): AuthenticationState {
        Log.d(TAG, "Starting authentication prompt for $packageName")
        return stateMachine.transitionTo(packageName, AuthState.PROMPTING)
    }

    /**
     * Mark an app as successfully authenticated
     */
    fun markAuthenticated(packageName: String): AuthenticationState {
        Log.d(TAG, "Marking $packageName as authenticated")
        // Set to never expire initially (Long.MAX_VALUE), will be updated on app exit
        return stateMachine.transitionTo(packageName, AuthState.AUTHENTICATED, Long.MAX_VALUE)
    }

    /**
     * Mark authentication as failed for an app
     */
    fun markAuthenticationFailed(packageName: String): AuthenticationState {
        Log.d(TAG, "Authentication failed for $packageName")
        return stateMachine.transitionTo(packageName, AuthState.UNAUTHENTICATED)
    }

    /**
     * Update expiration time when user exits an app
     */
    fun updateExpirationForAppExit(packageName: String) {
        val currentState = stateMachine.getState(packageName)
        if (currentState.state == AuthState.AUTHENTICATED && currentState.expirationTime == Long.MAX_VALUE) {
            val idleTimeout = IdleTimeoutCache.getIdleTimeout(context)
            val newExpiration = System.currentTimeMillis() + idleTimeout
            stateMachine.setExpiration(packageName, newExpiration)
            Log.d(TAG, "Set idle timeout for $packageName until $newExpiration")
        }
    }

    /**
     * Clear authentication for a specific app
     */
    fun clearAuthentication(packageName: String) {
        Log.d(TAG, "Clearing authentication for $packageName")
        stateMachine.clearState(packageName)
    }

    /**
     * Clear all authentication states (e.g., when changing authenticator)
     */
    fun clearAllAuthentications() {
        Log.d(TAG, "Clearing all authentication states")
        stateMachine.clearAllStates()
    }

    /**
     * Get current state for debugging
     */
    fun getCurrentState(packageName: String): AuthenticationState {
        return stateMachine.getState(packageName)
    }

    /**
     * Get all states for monitoring
     */
    fun getAllStates(): Map<String, AuthenticationState> {
        return stateMachine.getAllStates()
    }

    /**
     * Register a listener for state changes
     */
    fun registerStateListener(listener: AuthStateListener) {
        stateMachine.registerListener(listener)
    }

    /**
     * Unregister a state change listener
     */
    fun unregisterStateListener(listener: AuthStateListener) {
        stateMachine.unregisterListener(listener)
    }

    /**
     * Check if any apps have expired and clean them up
     */
    fun cleanupExpiredStates() {
        val allStates = stateMachine.getAllStates()
        val expiredPackages = allStates.filter { (_, state) -> 
            state.isExpired() && state.state == AuthState.AUTHENTICATED 
        }.keys
        
        expiredPackages.forEach { packageName ->
            Log.d(TAG, "Cleaning up expired authentication for $packageName")
            stateMachine.transitionTo(packageName, AuthState.UNAUTHENTICATED)
        }
    }
}