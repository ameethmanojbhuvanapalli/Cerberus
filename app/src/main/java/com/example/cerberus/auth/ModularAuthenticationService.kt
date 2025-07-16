package com.example.cerberus.auth

import android.content.Context
import android.util.Log
import com.example.cerberus.auth.authenticator.Authenticator
import com.example.cerberus.auth.flow.AuthenticationFlowController
import com.example.cerberus.auth.listeners.AuthFlowListener
import com.example.cerberus.auth.manager.AuthenticationStateManager

/**
 * Refactored AuthenticationService using modular components.
 * This maintains backward compatibility while using the new architecture.
 */
class ModularAuthenticationService(
    context: Context,
    private var authenticator: Authenticator
) {
    private val appContext = context.applicationContext
    private val stateManager = AuthenticationStateManager(appContext)
    private val flowController = AuthenticationFlowController(appContext, stateManager, authenticator)
    private val pendingCallbacks = mutableMapOf<AuthChannel, AuthenticationCallback>()
    private val TAG = "ModularAuthService"

    init {
        // Register internal flow listener to bridge with old callback system
        flowController.registerFlowListener(object : AuthFlowListener {
            override fun onFlowStarted(packageName: String) {
                Log.d(TAG, "Flow started for $packageName")
            }

            override fun onFlowCompleted(packageName: String, success: Boolean) {
                Log.d(TAG, "Flow completed for $packageName: success=$success")
                handleFlowCompleted(packageName, success)
            }

            override fun onFlowError(packageName: String, error: Exception) {
                Log.e(TAG, "Flow error for $packageName", error)
                handleFlowCompleted(packageName, false)
            }
        })

        // Register authenticator callback to bridge with flow controller
        authenticator.registerCallback(object : AuthenticationCallback {
            override fun onAuthenticationSucceeded(packageName: String) {
                Log.d(TAG, "Authenticator success for $packageName")
                flowController.handleAuthenticationSuccess(packageName)
            }

            override fun onAuthenticationFailed(packageName: String) {
                Log.d(TAG, "Authenticator failure for $packageName")
                flowController.handleAuthenticationFailure(packageName)
            }
        })
    }

    /**
     * Backward compatible API for requesting authentication
     */
    fun requestAuthenticationIfNeeded(
        channel: AuthChannel,
        packageName: String,
        callback: AuthenticationCallback
    ): Boolean {
        // Check if already authenticated
        if (stateManager.isAuthenticated(packageName)) {
            Log.d(TAG, "requestAuthenticationIfNeeded: $channel: Skipping $packageName, already authenticated")
            callback.onAuthenticationSucceeded(packageName)
            return false
        }

        Log.d(TAG, "requestAuthenticationIfNeeded: $channel: Triggering authentication for $packageName")
        
        // Store callback for this channel
        pendingCallbacks[channel] = callback
        
        // Start flow
        return flowController.startAuthenticationFlow(channel, packageName)
    }

    /**
     * Check if a package is authenticated
     */
    fun isAuthenticated(packageName: String): Boolean {
        return stateManager.isAuthenticated(packageName)
    }

    /**
     * Update expiration when app exits
     */
    fun updateExpirationForAppExit(packageName: String) {
        stateManager.updateExpirationForAppExit(packageName)
    }

    /**
     * Clear all authenticated apps
     */
    fun clearAuthenticatedApps() {
        stateManager.clearAllAuthentications()
        pendingCallbacks.clear()
    }

    /**
     * Update authenticator
     */
    fun updateAuthenticator(newAuthenticator: Authenticator) {
        Log.d(TAG, "Updating authenticator to ${newAuthenticator::class.java.simpleName}")
        
        // Unregister old authenticator callback
        authenticator.unregisterCallback(object : AuthenticationCallback {
            override fun onAuthenticationSucceeded(packageName: String) {}
            override fun onAuthenticationFailed(packageName: String) {}
        })
        
        // Update flow controller
        flowController.updateAuthenticator(newAuthenticator)
        
        // Update local reference
        authenticator = newAuthenticator
        
        // Register new authenticator callback
        authenticator.registerCallback(object : AuthenticationCallback {
            override fun onAuthenticationSucceeded(packageName: String) {
                Log.d(TAG, "New authenticator success for $packageName")
                flowController.handleAuthenticationSuccess(packageName)
            }

            override fun onAuthenticationFailed(packageName: String) {
                Log.d(TAG, "New authenticator failure for $packageName")
                flowController.handleAuthenticationFailure(packageName)
            }
        })
    }

    /**
     * Shutdown service
     */
    fun shutdown() {
        Log.d(TAG, "Shutting down modular authentication service")
        flowController.shutdown()
        pendingCallbacks.clear()
    }

    /**
     * Handle flow completion and notify pending callbacks
     */
    private fun handleFlowCompleted(packageName: String, success: Boolean) {
        // Find and notify all channels waiting for this package
        val channelsToNotify = pendingCallbacks.filter { (_, _) ->
            // For now, notify all pending callbacks since we don't track which package each channel is waiting for
            // This could be improved by tracking package per channel
            true
        }

        channelsToNotify.forEach { (channel, callback) ->
            try {
                if (success) {
                    callback.onAuthenticationSucceeded(packageName)
                } else {
                    callback.onAuthenticationFailed(packageName)
                }
                pendingCallbacks.remove(channel)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying callback for channel $channel", e)
            }
        }
    }

    /**
     * Get state manager for advanced usage
     */
    fun getStateManager(): AuthenticationStateManager = stateManager

    /**
     * Get flow controller for advanced usage
     */
    fun getFlowController(): AuthenticationFlowController = flowController
}