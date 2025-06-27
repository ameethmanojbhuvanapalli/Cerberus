package com.example.cerberus.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.auth.Authenticator
import com.example.cerberus.auth.AuthenticationManager
import com.example.cerberus.data.LockedAppsCache

class AppLockService : AccessibilityService(), AuthenticationCallback {

    private var lastLockedApp: String? = null
    private var isAuthenticated = false
    private lateinit var authenticator: Authenticator

    private val TAG = "AppLockService"

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Get the authenticator to use
        authenticator = AuthenticationManager.getInstance(this).getCurrentAuthenticator()
        authenticator.registerCallback(this)
        Log.d(TAG, "Service connected, authenticator initialized")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val foregroundApp = event.packageName?.toString()
            Log.d(TAG, "Window state changed: $foregroundApp")

            if (foregroundApp == packageName || foregroundApp == "com.example.cerberus.utils.BiometricPromptActivity") {
                Log.d(TAG, "Ignoring our own app")
                return
            }

            val lockedApps = LockedAppsCache.getLockedApps(this)
            Log.d(TAG, "Locked apps: ${lockedApps.joinToString()}")

            if (foregroundApp != null) {
                if (lockedApps.contains(foregroundApp)) {
                    Log.d(TAG, "Detected locked app: $foregroundApp")
                    Log.d(TAG, "isAuthenticated: $isAuthenticated, lastLockedApp: $lastLockedApp")

                    if (!isAuthenticated || foregroundApp != lastLockedApp) {
                        lastLockedApp = foregroundApp
                        isAuthenticated = false
                        Log.d(TAG, "Triggering authentication for: $foregroundApp")
                        authenticator.authenticate(this, foregroundApp)
                    } else {
                        Log.d(TAG, "Already authenticated for this app")
                    }
                } else {
                    lastLockedApp = null
                    isAuthenticated = false
                    Log.d(TAG, "App not locked: $foregroundApp")
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        Log.d(TAG, "Service being destroyed")
        authenticator.unregisterCallback(this)
        super.onDestroy()
    }

    // Authentication callback implementations
    override fun onAuthenticationSucceeded(packageName: String) {
        Log.d(TAG, "Authentication succeeded for: $packageName")
        if (packageName == lastLockedApp) {
            isAuthenticated = true
            Log.d(TAG, "Set isAuthenticated to true")
        }
    }

    override fun onAuthenticationFailed(packageName: String) {
        Log.d(TAG, "Authentication failed for: $packageName")
        isAuthenticated = false
    }
}