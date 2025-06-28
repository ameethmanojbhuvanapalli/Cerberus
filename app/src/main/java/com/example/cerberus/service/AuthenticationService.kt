package com.example.cerberus.service

import android.content.Context
import android.util.Log
import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.auth.AuthenticationManager
import com.example.cerberus.auth.Authenticator
import java.util.concurrent.ConcurrentHashMap

/**
 * Service to handle authentication operations.
 *
 * IMPORTANT: This class should be instantiated with application context
 * to prevent memory leaks.
 */
class AuthenticationService(context: Context) {
    // Always use application context to prevent leaks
    private val appContext = context.applicationContext
    private val authManager = AuthenticationManager.getInstance(appContext)
    private val authenticator: Authenticator
    private val authenticatedApps = ConcurrentHashMap<String, Long>()
    private val pendingAuthApps = ConcurrentHashMap.newKeySet<String>() // Track pending auth per package
    private val callbacks = mutableListOf<AuthenticationCallback>()
    private val TAG = "AuthenticationService"

    // Store a reference to our internal callback
    private val internalCallback: AuthenticationCallback

    private val IDLE_TIMEOUT_MS = 15 * 1000L

    init {
        authenticator = authManager.getCurrentAuthenticator()

        // Create and store the callback
        internalCallback = object : AuthenticationCallback {
            override fun onAuthenticationSucceeded(packageName: String) {
                handleAuthenticationSuccess(packageName)
            }

            override fun onAuthenticationFailed(packageName: String) {
                handleAuthenticationFailure(packageName)
            }
        }

        // Register the callback
        authenticator.registerCallback(internalCallback)

        Log.d(TAG, "Authentication service initialized")
    }

    /**
     * Request authentication for the given package if not already authenticated or pending.
     * Returns true if authentication prompt was launched, false otherwise.
     */
    fun requestAuthenticationIfNeeded(packageName: String): Boolean {
        val authTime = authenticatedApps[packageName]
        val now = System.currentTimeMillis()
        if (authTime != null && now <= authTime) {
            Log.d(TAG, "Skipping authentication for $packageName: already authenticated until $authTime, now=$now")
            return false
        }

        if (pendingAuthApps.contains(packageName)) {
            Log.d(TAG, "Skipping authentication for $packageName: authentication already pending")
            return false
        }

        Log.d(TAG, "Requesting authentication for $packageName")
        pendingAuthApps.add(packageName)
        authenticator.authenticate(appContext, packageName)
        return true
    }

    fun registerCallback(callback: AuthenticationCallback) {
        synchronized(callbacks) {
            if (!callbacks.contains(callback)) {
                callbacks.add(callback)
                Log.d(TAG, "Callback registered, total callbacks: ${callbacks.size}")
            }
        }
    }

    fun unregisterCallback(callback: AuthenticationCallback) {
        synchronized(callbacks) {
            callbacks.remove(callback)
            Log.d(TAG, "Callback unregistered, remaining callbacks: ${callbacks.size}")
        }
    }

    fun cleanupExpiredEntries() {
        val currentTime = System.currentTimeMillis()
        val iterator = authenticatedApps.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value != Long.MAX_VALUE && entry.value < currentTime) {
                iterator.remove()
            }
        }
    }

    fun updateExpirationForAppExit(packageName: String) {
        authenticatedApps[packageName]?.let {
            if (it == Long.MAX_VALUE) {
                authenticatedApps[packageName] = System.currentTimeMillis() + IDLE_TIMEOUT_MS
            }
        }
    }

    private fun handleAuthenticationSuccess(packageName: String) {
        Log.d(TAG, "Authentication succeeded for $packageName")
        authenticatedApps[packageName] = Long.MAX_VALUE
        pendingAuthApps.remove(packageName) // Remove from pending

        // Notify callbacks
        synchronized(callbacks) {
            callbacks.forEach { it.onAuthenticationSucceeded(packageName) }
        }
    }

    private fun handleAuthenticationFailure(packageName: String) {
        Log.d(TAG, "Authentication failed for $packageName")
        authenticatedApps.remove(packageName)
        pendingAuthApps.remove(packageName) // Remove from pending

        // Notify callbacks
        synchronized(callbacks) {
            callbacks.forEach { it.onAuthenticationFailed(packageName) }
        }
    }

    fun shutdown() {
        authenticator.unregisterCallback(internalCallback)
    }
}