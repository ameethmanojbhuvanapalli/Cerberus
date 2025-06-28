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
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundApp = event.packageName?.toString() ?: return

        // Skip authentication check if the BiometricPromptActivity is showing
        if (foregroundApp == myPackageName && foregroundApp.contains(promptActivityName)) return

        if (currentForegroundApp != foregroundApp) {
            // True app switch - from one package to another
            if (currentForegroundApp != null) {
                authService.updateExpirationForAppExit(currentForegroundApp!!)
            }

            // Only perform cleanup when truly switching apps
            if (foregroundApp != myPackageName) {
                authService.cleanupExpiredEntries()
            }

            currentForegroundApp = foregroundApp

            val lockedApps = LockedAppsCache.getLockedApps(this)

            val needsAuth = lockedApps.contains(foregroundApp) || foregroundApp == myPackageName
            if (needsAuth) {
                // Request authentication if needed
                authService.requestAuthenticationIfNeeded(foregroundApp)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        currentForegroundApp?.let { authService.updateExpirationForAppExit(it) }
        authService.shutdown()
        super.onDestroy()
    }
}