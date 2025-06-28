package com.example.cerberus.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.cerberus.data.LockedAppsCache

class AppLockService : AccessibilityService() {
    private var lastPackageName: String? = null
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
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundPackage = event.packageName?.toString() ?: return

        // Ignore authentication prompt activity
        if (foregroundPackage == myPackageName && event.className?.toString()?.contains(promptActivityName) == true) return

        // Always treat our own package as locked
        val lockedApps = LockedAppsCache.getLockedApps(this).toMutableSet().apply { add(myPackageName) }

        if (lastPackageName != null && lastPackageName != foregroundPackage) {
            if (lockedApps.contains(lastPackageName)) {
                authService.updateExpirationForAppExit(lastPackageName!!)
            }
        }

        if (lastPackageName != foregroundPackage) {
            if (lockedApps.contains(foregroundPackage)) {
                authService.requestAuthenticationIfNeeded(foregroundPackage)
            }
        }

        lastPackageName = foregroundPackage
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        lastPackageName?.let { authService.updateExpirationForAppExit(it) }
        authService.shutdown()
        super.onDestroy()
    }
}