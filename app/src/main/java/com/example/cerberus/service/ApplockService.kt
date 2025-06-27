package com.example.cerberus.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.auth.Authenticator
import com.example.cerberus.auth.AuthenticationManager
import com.example.cerberus.data.LockedAppsCache
import java.util.concurrent.ConcurrentHashMap

class AppLockService : AccessibilityService(), AuthenticationCallback {

    private val authenticatedApps = ConcurrentHashMap<String, Long>()
    private var currentForegroundApp: String? = null
    private var pendingAuthentication: String? = null

    private lateinit var authenticator: Authenticator

    private val IDLE_TIMEOUT_MS = 15 * 1000L
    private val TAG = "AppLockService"

    private lateinit var myPackageName: String
    private val promptActivityName = "com.example.cerberus.utils.BiometricPromptActivity"

    override fun onServiceConnected() {
        super.onServiceConnected()
        myPackageName = packageName

        // Initialize the authenticator
        authenticator = AuthenticationManager.getInstance(this).getCurrentAuthenticator()
        authenticator.registerCallback(this)

        Log.d(TAG, "Service connected, authenticator initialized")
        cleanupExpiredEntries()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Early return if not window state changed event
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundApp = event.packageName?.toString() ?: return

        // Skip authentication check if the BiometricPromptActivity is showing
        if (foregroundApp == promptActivityName) return

        if (currentForegroundApp != null && currentForegroundApp != foregroundApp) {
            updateExpirationForAppExit(currentForegroundApp!!)

            // Only perform cleanup when truly switching apps
            if (authenticatedApps.size > 10) {
                cleanupExpiredEntries()
            }
        }

        // Update current foreground app (regardless if it's a new activity or new app)
        currentForegroundApp = foregroundApp

        val lockedApps = LockedAppsCache.getLockedApps(this)

        val needsAuth = lockedApps.contains(foregroundApp) || foregroundApp == myPackageName
        if (!needsAuth) return

        // Handle authentication flow
        if (foregroundApp == myPackageName && foregroundApp == pendingAuthentication) {
            pendingAuthentication = null
            return
        }

        // Fast path: check if authenticated and return quickly if so
        val authTime = authenticatedApps[foregroundApp]
        if (authTime != null && System.currentTimeMillis() <= authTime) return

        // Slow path: needs authentication
        pendingAuthentication = foregroundApp
        authenticator.authenticate(this, foregroundApp)
    }

    private fun cleanupExpiredEntries() {
        val currentTime = System.currentTimeMillis()
        val iterator = authenticatedApps.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value != Long.MAX_VALUE && entry.value < currentTime) {
                iterator.remove()
            }
        }
    }

    private fun updateExpirationForAppExit(packageName: String) {
        authenticatedApps[packageName]?.let {
            if (it == Long.MAX_VALUE) {
                authenticatedApps[packageName] = System.currentTimeMillis() + IDLE_TIMEOUT_MS
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        currentForegroundApp?.let { updateExpirationForAppExit(it) }
        if (::authenticator.isInitialized) {
            authenticator.unregisterCallback(this)
        }
        super.onDestroy()
    }

    override fun onAuthenticationSucceeded(packageName: String) {
        authenticatedApps[packageName] = Long.MAX_VALUE
        pendingAuthentication = null
    }

    override fun onAuthenticationFailed(packageName: String) {
        authenticatedApps.remove(packageName)
    }
}