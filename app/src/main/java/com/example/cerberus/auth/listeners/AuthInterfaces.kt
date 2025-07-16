package com.example.cerberus.auth.listeners

import com.example.cerberus.auth.state.AuthState
import com.example.cerberus.auth.state.AuthenticationState

/**
 * Listener for authentication state changes
 */
interface AuthStateListener {
    fun onStateChanged(oldState: AuthenticationState, newState: AuthenticationState)
}

/**
 * Listener for authentication flow events
 */
interface AuthFlowListener {
    fun onFlowStarted(packageName: String)
    fun onFlowCompleted(packageName: String, success: Boolean)
    fun onFlowError(packageName: String, error: Exception)
}

/**
 * Listener for app lifecycle events
 */
interface AppLifecycleListener {
    fun onAppEntered(packageName: String)
    fun onAppExited(packageName: String)
    fun onPromptActivityDetected(packageName: String, className: String)
}

/**
 * Interface for authentication providers
 */
interface AuthenticationProvider {
    fun authenticate(packageName: String): Boolean
    fun isAvailable(): Boolean
    fun getType(): String
}