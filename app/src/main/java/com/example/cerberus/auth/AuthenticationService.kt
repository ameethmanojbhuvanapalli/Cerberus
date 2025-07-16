package com.example.cerberus.auth

import android.content.Context
import android.util.Log
import com.example.cerberus.auth.authenticator.Authenticator
import com.example.cerberus.data.IdleTimeoutCache
import java.util.concurrent.ConcurrentHashMap

class AuthenticationService(
    context: Context,
    private var authenticator: Authenticator
) {
    private val appContext = context.applicationContext
    private val stateMachine = AuthenticationStateMachine()
    private val pendingCallbacks = mutableMapOf<AuthChannel, PendingRequest>()
    private val TAG = "AuthenticationService"

    private val IDLE_TIMEOUT_MS get() = IdleTimeoutCache.getIdleTimeout(appContext)

    private data class PendingRequest(val packageName: String, val callback: AuthenticationCallback)

    private val internalCallback: AuthenticationCallback = object : AuthenticationCallback {
        override fun onAuthenticationSucceeded(packageName: String) {
            Log.d(TAG, "internalCallback: onAuthenticationSucceeded for $packageName")
            
            // Update state machine
            stateMachine.processEvent(AuthenticationStateMachine.Event.AuthenticationSucceeded(packageName))
            
            // Notify all channels waiting for this package
            val channelsToClear = mutableListOf<AuthChannel>()
            for ((channel, request) in pendingCallbacks) {
                if (request.packageName == packageName) {
                    request.callback.onAuthenticationSucceeded(packageName)
                    channelsToClear.add(channel)
                }
            }
            channelsToClear.forEach { pendingCallbacks.remove(it) }
        }

        override fun onAuthenticationFailed(packageName: String) {
            Log.d(TAG, "internalCallback: onAuthenticationFailed for $packageName")
            
            // Update state machine - this keeps the prompt active for retries
            stateMachine.processEvent(AuthenticationStateMachine.Event.AuthenticationFailed(packageName))
            
            // Don't notify channels of failure yet - let prompt retry internally
            // Only notify on prompt destruction
        }
    }

    init {
        authenticator.registerCallback(internalCallback)
        
        // Listen for state machine transitions
        stateMachine.addStateChangeListener { packageName, oldState, newState ->
            Log.d(TAG, "State transition for $packageName: $oldState → $newState")
        }
    }

    fun requestAuthenticationIfNeeded(
        channel: AuthChannel,
        packageName: String,
        callback: AuthenticationCallback
    ): Boolean {
        // Check if already authenticated using state machine
        if (stateMachine.isAuthenticated(packageName, IDLE_TIMEOUT_MS)) {
            Log.d(TAG, "requestAuthenticationIfNeeded: $channel: Skipping $packageName, already authenticated")
            callback.onAuthenticationSucceeded(packageName)
            return false
        }
        
        // Check if prompt is already active
        if (stateMachine.isPromptActive(packageName)) {
            Log.d(TAG, "requestAuthenticationIfNeeded: $channel: Prompt already active for $packageName")
            // Replace any old pending request for this channel
            pendingCallbacks[channel] = PendingRequest(packageName, callback)
            return true // Prompt is active but not started by this call
        }
        
        Log.d(TAG, "requestAuthenticationIfNeeded: $channel: Triggering authentication for $packageName")
        
        // Replace any old pending request for this channel
        pendingCallbacks[channel] = PendingRequest(packageName, callback)
        
        // Update state machine to PROMPTING
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptRequested(packageName))
        
        // Start authentication
        authenticator.authenticate(appContext, packageName)
        return true
    }

    fun clearAuthenticatedApps() {
        stateMachine.clearAllStates()
    }

    fun isAuthenticated(packageName: String): Boolean {
        return stateMachine.isAuthenticated(packageName, IDLE_TIMEOUT_MS)
    }

    fun updateExpirationForAppExit(packageName: String) {
        // Only update timeout if currently authenticated
        if (stateMachine.getCurrentState(packageName) == AuthenticationStateMachine.State.AUTHENTICATED) {
            Log.d(TAG, "updateExpirationForAppExit: Setting idle timeout for $packageName")
            stateMachine.setAuthenticationTimeout(packageName, IDLE_TIMEOUT_MS)
            // Transition to UNAUTHENTICATED
            stateMachine.processEvent(AuthenticationStateMachine.Event.AppExited(packageName))
        }
    }

    fun updateAuthenticator(newAuthenticator: Authenticator) {
        authenticator.unregisterCallback(internalCallback)
        authenticator = newAuthenticator
        authenticator.registerCallback(internalCallback)
        clearAuthenticatedApps()
        Log.d(TAG, "updateAuthenticator: Switched to ${newAuthenticator::class.java.simpleName}")
    }

    fun shutdown() {
        clearAuthenticatedApps()
        pendingCallbacks.clear()
        authenticator.unregisterCallback(internalCallback)
        Log.d(TAG, "AuthenticationService: shutdown completed")
    }
    
    /**
     * Notify that a prompt was destroyed (called when prompt activity finishes)
     */
    fun notifyPromptDestroyed(packageName: String) {
        Log.d(TAG, "notifyPromptDestroyed for $packageName")
        
        // Update state machine
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptDestroyed(packageName))
        
        // Notify all channels waiting for this package about failure
        val channelsToClear = mutableListOf<AuthChannel>()
        for ((channel, request) in pendingCallbacks) {
            if (request.packageName == packageName) {
                request.callback.onAuthenticationFailed(packageName)
                channelsToClear.add(channel)
            }
        }
        channelsToClear.forEach { pendingCallbacks.remove(it) }
    }
}