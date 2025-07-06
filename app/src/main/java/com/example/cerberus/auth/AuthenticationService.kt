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
    private val callbacks = mutableListOf<AuthenticationCallback>()
    private val TAG = "AuthenticationService"

    private val internalCallback: AuthenticationCallback

    private val IDLE_TIMEOUT_MS get() = IdleTimeoutCache.getIdleTimeout(appContext)

    init {
        Log.d(TAG, "Initializing AuthenticationService")
        internalCallback = object : AuthenticationCallback {
            override fun onAuthenticationSucceeded(packageName: String) {
                Log.d(TAG, "internalCallback: onAuthenticationSucceeded for $packageName")
                handleAuthenticationSuccess(packageName)
            }

            override fun onAuthenticationFailed(packageName: String) {
                Log.d(TAG, "internalCallback: onAuthenticationFailed for $packageName")
                handleAuthenticationFailure(packageName)
            }
        }

        authenticator.registerCallback(internalCallback)
    }

    fun requestAuthenticationIfNeeded(packageName: String): Boolean {
        val authTime = authenticatedApps[packageName]
        val now = System.currentTimeMillis()
        if (authTime != null && now <= authTime) {
            Log.d(TAG, "requestAuthenticationIfNeeded: Skipping $packageName, already authenticated")
            authenticatedApps[packageName] = Long.MAX_VALUE
            return false
        }

        Log.d(TAG, "requestAuthenticationIfNeeded: Triggering authentication for $packageName")
        authenticator.authenticate(appContext, packageName)
        return true
    }

    fun registerCallback(callback: AuthenticationCallback) {
        synchronized(callbacks) {
            if (!callbacks.contains(callback)) {
                callbacks.add(callback)
                Log.d(TAG, "registerCallback: Added new callback, total = ${callbacks.size}")
            }
        }
    }

    fun unregisterCallback(callback: AuthenticationCallback) {
        synchronized(callbacks) {
            callbacks.remove(callback)
            Log.d(TAG, "unregisterCallback: Removed callback, total = ${callbacks.size}")
        }
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

    private fun handleAuthenticationSuccess(packageName: String) {
        authenticatedApps[packageName] = Long.MAX_VALUE
        synchronized(callbacks) {
            callbacks.forEach { it.onAuthenticationSucceeded(packageName) }
        }
    }

    private fun handleAuthenticationFailure(packageName: String) {
        authenticatedApps.remove(packageName)
        synchronized(callbacks) {
            callbacks.forEach { it.onAuthenticationFailed(packageName) }
        }
    }

    fun updateAuthenticator(newAuthenticator: Authenticator) {
        authenticator.unregisterCallback(internalCallback)
        authenticator = newAuthenticator
        authenticator.registerCallback(internalCallback)
        clearAuthenticatedApps()
        Log.d(TAG, "updateAuthenticator: Switched to ${newAuthenticator::class.java.simpleName}")
    }
}