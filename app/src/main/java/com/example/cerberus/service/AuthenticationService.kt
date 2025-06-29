package com.example.cerberus.service

import android.content.Context
import android.util.Log
import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.auth.AuthenticationManager
import com.example.cerberus.auth.Authenticator
import com.example.cerberus.auth.AuthenticatorChangeListener
import com.example.cerberus.data.IdleTimeoutCache
import java.util.concurrent.ConcurrentHashMap

class AuthenticationService(context: Context) : AuthenticatorChangeListener {
    private val appContext = context.applicationContext
    private val authManager = AuthenticationManager.getInstance(appContext)
    private var authenticator: Authenticator = authManager.getCurrentAuthenticator()
    private val authenticatedApps = ConcurrentHashMap<String, Long>()
    private val callbacks = mutableListOf<AuthenticationCallback>()
    private val TAG = "AuthenticationService"

    private val internalCallback: AuthenticationCallback

    private val IDLE_TIMEOUT_MS get() = IdleTimeoutCache.getIdleTimeout(appContext)

    init {
        Log.d(TAG, "Initializing AuthenticationService")
        authManager.registerListener(this)
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
        Log.d(TAG, "Registered internalCallback with ${authenticator::class.java.simpleName}")
    }

    companion object {
        @Volatile
        private var instance: AuthenticationService? = null

        fun getInstance(context: Context): AuthenticationService {
            return instance ?: synchronized(this) {
                instance ?: AuthenticationService(context.applicationContext).also { instance = it }
            }
        }
    }

    fun requestAuthenticationIfNeeded(packageName: String): Boolean {
        val authTime = authenticatedApps[packageName]
        val now = System.currentTimeMillis()
        if (authTime != null && now <= authTime) {
            Log.d(TAG, "requestAuthenticationIfNeeded: Skipping $packageName, already authenticated")
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

    fun cleanupExpiredEntries() {
        val currentTime = System.currentTimeMillis()
        val iterator = authenticatedApps.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value != Long.MAX_VALUE && entry.value < currentTime) {
                Log.d(TAG, "cleanupExpiredEntries: Removing expired entry for ${entry.key}")
                iterator.remove()
            }
        }
    }

    fun clearAuthenticatedApps() {
        val currentApp = appContext.packageName
        authenticatedApps.keys.removeIf { it != currentApp }
        Log.d(TAG, "clearAuthenticatedApps: Cleared all except $currentApp")
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
        Log.d(TAG, "handleAuthenticationSuccess: $packageName")
        authenticatedApps[packageName] = Long.MAX_VALUE
        synchronized(callbacks) {
            callbacks.forEach { it.onAuthenticationSucceeded(packageName) }
        }
    }

    private fun handleAuthenticationFailure(packageName: String) {
        Log.d(TAG, "handleAuthenticationFailure: $packageName")
        authenticatedApps.remove(packageName)
        synchronized(callbacks) {
            callbacks.forEach { it.onAuthenticationFailed(packageName) }
        }
    }

    override fun onAuthenticatorChanged(newAuthenticator: Authenticator) {
        authenticator.unregisterCallback(internalCallback)
        authenticator = newAuthenticator
        authenticator.registerCallback(internalCallback)
        Log.d(TAG, "onAuthenticatorChanged: Switched to ${newAuthenticator::class.java.simpleName}")
    }

    fun shutdown() {
        Log.d(TAG, "shutdown: Unregistering internal callback")
        authenticator.unregisterCallback(internalCallback)
    }
}
