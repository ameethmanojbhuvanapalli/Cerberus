package com.example.cerberus.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.cerberus.data.LockedAppsCache

class AppLockService : AccessibilityService() {
    private var currentForegroundApp: String? = null
    private lateinit var authService: AuthenticationService
    private val TAG = "AppLockService"
    private lateinit var myPackageName: String
    private val promptActivityName = "com.example.cerberus.utils.BiometricPromptActivity"

    override fun onServiceConnected() {
        super.onServiceConnected()
        myPackageName = packageName

        authService = AuthenticationService(applicationContext)

        Log.d(TAG, "Service connected")
        authService.cleanupExpiredEntries()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Early return if not window state changed event
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "Event type is not TYPE_WINDOW_STATE_CHANGED, ignoring.")
            return
        }

        Log.d(TAG, "Received event: type=${event.eventType}, package=${event.packageName}")

        val foregroundApp = event.packageName?.toString() ?: run {
            Log.d(TAG, "Foreground app packageName is null.")
            return
        }

        Log.d(TAG, "Foreground app: $foregroundApp, Previous foreground app: $currentForegroundApp")

        // Skip authentication check if the BiometricPromptActivity is showing
        if (foregroundApp == myPackageName && foregroundApp.contains(promptActivityName)) {
            Log.d(TAG, "Skipping authentication because BiometricPromptActivity is showing.")
            return
        }

        if (currentForegroundApp != foregroundApp) {
            Log.d(TAG, "Detected app switch from $currentForegroundApp to $foregroundApp")

            // True app switch - from one package to another
            if (currentForegroundApp != null) {
                Log.d(TAG, "Calling updateExpirationForAppExit for $currentForegroundApp")
                authService.updateExpirationForAppExit(currentForegroundApp!!)
            }

            // Only perform cleanup when truly switching apps
            if (foregroundApp != myPackageName) {
                Log.d(TAG, "Calling cleanupExpiredEntries (not our app in foreground)")
                authService.cleanupExpiredEntries()
            }

            currentForegroundApp = foregroundApp

            val lockedApps = LockedAppsCache.getLockedApps(this)
            Log.d(TAG, "Locked apps: $lockedApps")

            val needsAuth = lockedApps.contains(foregroundApp) || foregroundApp == myPackageName
            Log.d(TAG, "Needs authentication for $foregroundApp: $needsAuth")

            if (needsAuth) {
                Log.d(TAG, "Requesting authentication for $foregroundApp")
                authService.requestAuthenticationIfNeeded(foregroundApp)
            }
        } else {
            Log.d(TAG, "No app switch detected. currentForegroundApp = $currentForegroundApp")
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed, cleaning up.")
        currentForegroundApp?.let {
            Log.d(TAG, "Calling updateExpirationForAppExit for $it (onDestroy)")
            authService.updateExpirationForAppExit(it)
        }
        authService.shutdown()
        super.onDestroy()
    }
}