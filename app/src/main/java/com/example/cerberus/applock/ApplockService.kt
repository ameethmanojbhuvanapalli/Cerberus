package com.example.cerberus.applock

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.cerberus.auth.AuthenticationManager
import com.example.cerberus.data.LockedAppsCache
import com.example.cerberus.data.ProtectionCache

class AppLockService : AccessibilityService() {
    private var lastPackageName: String? = null
    private var lastClassName: String? = null
    private val TAG = "AppLockService"
    private lateinit var myPackageName: String
    private val promptActivityName = "com.example.cerberus.utils.BiometricPromptActivity"

    private val handler = Handler(Looper.getMainLooper())
    private var stablePromptRunnable: Runnable? = null
    private var stableSince: Long = 0L
    private var activityChangeCount: Int = 0
    private val STABLE_DELAY = 500L

    private val authService
        get() = AuthenticationManager.getInstance(applicationContext).getAuthService()

    override fun onServiceConnected() {
        super.onServiceConnected()
        myPackageName = packageName
        Log.d(TAG, "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isProtectionEnabled()) return
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundPackage = event.packageName?.toString() ?: return
        val foregroundClass = event.className?.toString() ?: return

        // Don't prompt if prompt activity is being shown
        if (foregroundPackage == myPackageName && foregroundClass.contains(promptActivityName)) {
            lastPackageName = foregroundPackage
            lastClassName = foregroundClass
            return
        }

        val lockedApps = LockedAppsCache.getLockedApps(this).toMutableSet().apply { add(myPackageName) }

        // If we moved from a locked app to another app, update expiration
        if (lastPackageName != null && lastPackageName != foregroundPackage) {
            if (lockedApps.contains(lastPackageName)) {
                authService.updateExpirationForAppExit(lastPackageName!!)
            }
        }

        if (
            lockedApps.contains(foregroundPackage) &&
            lastPackageName != null &&
            lastPackageName != foregroundPackage &&
            !authService.isAuthenticated(foregroundPackage)
        ) {
            activityChangeCount = 1
            stableSince = System.currentTimeMillis()

            stablePromptRunnable?.let { handler.removeCallbacks(it) }
            stablePromptRunnable = Runnable {
                // Only prompt if still in the same package/class as when scheduled
                if (
                    lockedApps.contains(foregroundPackage) &&
                    lastPackageName == foregroundPackage &&
                    lastClassName == foregroundClass
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
        return ProtectionCache.isProtectionEnabled(this)
    }
}