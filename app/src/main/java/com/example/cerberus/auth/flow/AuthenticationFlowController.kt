package com.example.cerberus.auth.flow

import android.content.Context
import android.util.Log
import com.example.cerberus.auth.AuthChannel
import com.example.cerberus.auth.authenticator.Authenticator
import com.example.cerberus.auth.listeners.AuthFlowListener
import com.example.cerberus.auth.manager.AuthenticationStateManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Controls authentication flows and coordinates with authenticators.
 * Manages prompt lifecycle, authenticator coordination, and flow events.
 */
class AuthenticationFlowController(
    private val context: Context,
    private val stateManager: AuthenticationStateManager,
    private var authenticator: Authenticator
) {
    private val activeFlows = ConcurrentHashMap<String, FlowContext>()
    private val flowListeners = mutableListOf<AuthFlowListener>()
    private val pendingRequests = ConcurrentHashMap<AuthChannel, PendingRequest>()
    
    companion object {
        private const val TAG = "AuthFlowController"
    }

    private data class FlowContext(
        val packageName: String,
        val channel: AuthChannel,
        val startTime: Long = System.currentTimeMillis()
    )

    private data class PendingRequest(
        val packageName: String,
        val flowContext: FlowContext
    )

    /**
     * Start an authentication flow for a package
     */
    fun startAuthenticationFlow(
        channel: AuthChannel,
        packageName: String
    ): Boolean {
        Log.d(TAG, "Starting authentication flow for $packageName via $channel")
        
        // Check if already authenticated
        if (stateManager.isAuthenticated(packageName)) {
            Log.d(TAG, "Package $packageName already authenticated")
            notifyFlowCompleted(packageName, true)
            return false
        }

        // Check if flow already active for this package
        if (activeFlows.containsKey(packageName)) {
            Log.d(TAG, "Authentication flow already active for $packageName")
            return false
        }

        val flowContext = FlowContext(packageName, channel)
        activeFlows[packageName] = flowContext
        pendingRequests[channel] = PendingRequest(packageName, flowContext)

        // Update state machine
        stateManager.startPrompting(packageName)

        // Notify listeners
        notifyFlowStarted(packageName)

        // Trigger authenticator
        try {
            authenticator.authenticate(context, packageName)
            Log.d(TAG, "Authenticator triggered for $packageName")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting authentication for $packageName", e)
            handleFlowError(packageName, e)
            return false
        }
    }

    /**
     * Handle successful authentication
     */
    fun handleAuthenticationSuccess(packageName: String) {
        Log.d(TAG, "Handling authentication success for $packageName")
        
        val flowContext = activeFlows.remove(packageName)
        if (flowContext != null) {
            // Remove pending request for this channel
            pendingRequests.remove(flowContext.channel)
            
            // Update state
            stateManager.markAuthenticated(packageName)
            
            // Notify listeners
            notifyFlowCompleted(packageName, true)
        } else {
            Log.w(TAG, "No active flow found for successful authentication: $packageName")
        }
    }

    /**
     * Handle failed authentication
     */
    fun handleAuthenticationFailure(packageName: String) {
        Log.d(TAG, "Handling authentication failure for $packageName")
        
        val flowContext = activeFlows.remove(packageName)
        if (flowContext != null) {
            // Remove pending request for this channel
            pendingRequests.remove(flowContext.channel)
            
            // Update state
            stateManager.markAuthenticationFailed(packageName)
            
            // Notify listeners
            notifyFlowCompleted(packageName, false)
        } else {
            Log.w(TAG, "No active flow found for failed authentication: $packageName")
        }
    }

    /**
     * Handle authentication error
     */
    private fun handleFlowError(packageName: String, error: Exception) {
        Log.e(TAG, "Handling authentication error for $packageName", error)
        
        val flowContext = activeFlows.remove(packageName)
        if (flowContext != null) {
            // Remove pending request for this channel
            pendingRequests.remove(flowContext.channel)
            
            // Update state  
            stateManager.markAuthenticationFailed(packageName)
            
            // Notify listeners
            notifyFlowError(packageName, error)
        }
    }

    /**
     * Cancel an active authentication flow
     */
    fun cancelFlow(packageName: String) {
        Log.d(TAG, "Cancelling authentication flow for $packageName")
        
        val flowContext = activeFlows.remove(packageName)
        if (flowContext != null) {
            pendingRequests.remove(flowContext.channel)
            stateManager.markAuthenticationFailed(packageName)
            notifyFlowCompleted(packageName, false)
        }
    }

    /**
     * Check if a flow is active for a package
     */
    fun isFlowActive(packageName: String): Boolean {
        return activeFlows.containsKey(packageName)
    }

    /**
     * Get active flows (for monitoring)
     */
    fun getActiveFlows(): Map<String, FlowContext> {
        return activeFlows.toMap()
    }

    /**
     * Update the authenticator (when user changes auth method)
     */
    fun updateAuthenticator(newAuthenticator: Authenticator) {
        Log.d(TAG, "Updating authenticator to ${newAuthenticator::class.java.simpleName}")
        
        // Cancel all active flows
        val activePackages = activeFlows.keys.toList()
        activePackages.forEach { cancelFlow(it) }
        
        // Update authenticator
        authenticator = newAuthenticator
        
        // Clear all authentication states
        stateManager.clearAllAuthentications()
    }

    /**
     * Register a flow listener
     */
    fun registerFlowListener(listener: AuthFlowListener) {
        if (!flowListeners.contains(listener)) {
            flowListeners.add(listener)
            Log.d(TAG, "Registered flow listener: ${listener::class.java.simpleName}")
        }
    }

    /**
     * Unregister a flow listener
     */
    fun unregisterFlowListener(listener: AuthFlowListener) {
        flowListeners.remove(listener)
        Log.d(TAG, "Unregistered flow listener: ${listener::class.java.simpleName}")
    }

    private fun notifyFlowStarted(packageName: String) {
        flowListeners.forEach { listener ->
            try {
                listener.onFlowStarted(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying flow started", e)
            }
        }
    }

    private fun notifyFlowCompleted(packageName: String, success: Boolean) {
        flowListeners.forEach { listener ->
            try {
                listener.onFlowCompleted(packageName, success)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying flow completed", e)
            }
        }
    }

    private fun notifyFlowError(packageName: String, error: Exception) {
        flowListeners.forEach { listener ->
            try {
                listener.onFlowError(packageName, error)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying flow error", e)
            }
        }
    }

    /**
     * Shutdown and cleanup
     */
    fun shutdown() {
        Log.d(TAG, "Shutting down authentication flow controller")
        
        // Cancel all active flows
        val activePackages = activeFlows.keys.toList()
        activePackages.forEach { cancelFlow(it) }
        
        // Clear listeners
        flowListeners.clear()
        pendingRequests.clear()
    }
}