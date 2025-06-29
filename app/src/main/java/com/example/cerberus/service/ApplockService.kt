package com.example.cerberus.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.cerberus.data.LockedAppsCache

class AppLockService : AccessibilityService() {
    private var lastPackageName: String? = null
    private var lastClassName: String? = null
    private lateinit var authService: AuthenticationService
    private val TAG = "AppLockService"
    private lateinit var myPackageName: String
    private val promptActivityName = "com.example.cerberus.utils.BiometricPromptActivity"

    // Heuristic state
    private val handler = Handler(Looper.getMainLooper())
    private var stablePromptRunnable: Runnable? = null
    private var stableSince: Long = 0L
    private var activityChangeCount: Int = 0
    private val STABLE_DELAY = 500L

    override fun onServiceConnected() {
        super.onServiceConnected()
        myPackageName = packageName
        authService = AuthenticationService.getInstance(applicationContext)
        Log.d(TAG, "Service connected")
        authService.cleanupExpiredEntries()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isProtectionEnabled()) return
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundPackage = event.packageName?.toString() ?: return
        val foregroundClass = event.className?.toString() ?: return

        // Ignore authentication prompt activity
        if (foregroundPackage == myPackageName && foregroundClass.contains(promptActivityName)) return

        // Always treat our own package as locked
        val lockedApps = LockedAppsCache.getLockedApps(this).toMutableSet().apply { add(myPackageName) }

        // Handle app exit update
        if (lastPackageName != null && lastPackageName != foregroundPackage) {
            if (lockedApps.contains(lastPackageName)) {
                authService.updateExpirationForAppExit(lastPackageName!!)
            }
        }

        // Heuristic: Only prompt after activity settles
        if (lockedApps.contains(foregroundPackage)) {
            // If package or class changed, reset timer
            if (lastPackageName != foregroundPackage) {
                activityChangeCount = 1
                stableSince = System.currentTimeMillis()
            } else if (lastClassName != foregroundClass) {
                activityChangeCount++
                stableSince = System.currentTimeMillis()
            }

            // Cancel previous scheduled prompt
            stablePromptRunnable?.let { handler.removeCallbacks(it) }
            stablePromptRunnable = Runnable {
                // Only prompt if still on same package/class as when scheduled
                if (lockedApps.contains(foregroundPackage)
                    && lastPackageName == foregroundPackage
                    && lastClassName == foregroundClass
                ) {
                    Log.d(TAG, "Prompting after $activityChangeCount activity changes and ${System.currentTimeMillis() - stableSince}ms dwell")
                    authService.requestAuthenticationIfNeeded(foregroundPackage)
                }
            }
            handler.postDelayed(stablePromptRunnable!!, STABLE_DELAY)
        }

        lastPackageName = foregroundPackage
        lastClassName = foregroundClass
    }

    override fun onInterrupt() {}

    private fun isProtectionEnabled(): Boolean {
        return getSharedPreferences("settings", MODE_PRIVATE)
            .getBoolean("protection_enabled", false)
    }

    override fun onDestroy() {
        lastPackageName?.let { authService.updateExpirationForAppExit(it) }
        authService.shutdown()
        super.onDestroy()
    }
}