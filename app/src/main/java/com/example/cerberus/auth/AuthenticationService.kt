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
    private val authenticatedApps = ConcurrentHashMap<String, Long>()
    private val pendingCallbacks = mutableMapOf<AuthChannel, PendingRequest>()
    private val TAG = "AuthenticationService"

    private val IDLE_TIMEOUT_MS get() = IdleTimeoutCache.getIdleTimeout(appContext)

    private data class PendingRequest(val packageName: String, val callback: AuthenticationCallback)

    private val internalCallback: AuthenticationCallback = object : AuthenticationCallback {
        override fun onAuthenticationSucceeded(packageName: String) {
            Log.d(TAG, "internalCallback: onAuthenticationSucceeded for $packageName")
            authenticatedApps[packageName] = Long.MAX_VALUE
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
            authenticatedApps.remove(packageName)
            // Notify all channels waiting for this package
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

    init {
        authenticator.registerCallback(internalCallback)
    }

    fun requestAuthenticationIfNeeded(
        channel: AuthChannel,
        packageName: String,
        callback: AuthenticationCallback
    ): Boolean {
        val authTime = authenticatedApps[packageName]
        val now = System.currentTimeMillis()
        if (authTime != null && now <= authTime) {
            Log.d(TAG, "requestAuthenticationIfNeeded: $channel: Skipping $packageName, already authenticated")
            callback.onAuthenticationSucceeded(packageName)
            return false
        }
        Log.d(TAG, "requestAuthenticationIfNeeded: $channel: Triggering authentication for $packageName")
        // Replace any old pending request for this channel
        pendingCallbacks[channel] = PendingRequest(packageName, callback)
        authenticator.authenticate(appContext, packageName)
        return true
    }

    fun clearAuthenticatedApps() {
        authenticatedApps.clear()
    }

    fun isAuthenticated(packageName: String): Boolean {
        val authTime = authenticatedApps[packageName]
        val now = System.currentTimeMillis()
        return authTime != null && now <= authTime
    }

    fun updateExpirationForAppExit(packageName: String) {
        authenticatedApps[packageName]?.let {
            if (it == Long.MAX_VALUE) {
                val newExpiration = System.currentTimeMillis() + IDLE_TIMEOUT_MS
                Log.d(TAG, "updateExpirationForAppExit: Setting idle timeout for $packageName until $newExpiration")
                authenticatedApps[packageName] = newExpiration
            }
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
}