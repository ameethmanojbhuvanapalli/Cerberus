package com.example.cerberus.applock

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.auth.AuthenticationManager
import com.example.cerberus.auth.AuthChannel
import com.example.cerberus.data.LockedAppsCache
import com.example.cerberus.data.ProtectionCache

class AppLockService : AccessibilityService() {
    private var lastPackageName: String? = null
    private var lastClassName: String? = null
    private val TAG = "AppLockService"
    private lateinit var myPackageName: String
    // All Cerberus authentication prompt activities that should be ignored
    private val cerberusPromptActivities = setOf(
        "BiometricPromptActivity",
        "PinPromptActivity", 
        "PasswordPromptActivity",
        "PatternPromptActivity"
    )
    private val systemPackages = setOf("com.android.systemui", "android", null)

    private val handler = Handler(Looper.getMainLooper())
    private var stablePromptRunnable: Runnable? = null
    private var stableSince: Long = 0L
    private var activityChangeCount: Int = 0
    private val STABLE_DELAY = 500L

    // Debounce for app exit
    private var appExitRunnable: Runnable? = null
    private val APP_EXIT_DELAY = 1500L // ms (tweak as needed)
    private var pendingAppExitPackage: String? = null

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

        if (systemPackages.contains(foregroundPackage)) return

        // Completely ignore all Cerberus authentication prompt activities
        if (foregroundPackage == myPackageName && isCerberusPromptActivity(foregroundClass)) {
            Log.d(TAG, "Ignoring Cerberus prompt activity: $foregroundClass")
            lastPackageName = foregroundPackage
            lastClassName = foregroundClass
            return
        }

        // Get locked apps but do NOT include Cerberus itself to prevent authentication loops
        val lockedApps = LockedAppsCache.getLockedApps(this)

        // Debounced expiration update logic
        if (
            lastPackageName != null &&
            lastPackageName != foregroundPackage &&
            lockedApps.contains(lastPackageName)
        ) {
            // User appears to have left a locked app
            pendingAppExitPackage = lastPackageName
            // Cancel any previous pending exit
            appExitRunnable?.let { handler.removeCallbacks(it) }
            appExitRunnable = Runnable {
                // Only update expiration if user did NOT return to the locked app
                if (pendingAppExitPackage != foregroundPackage) {
                    Log.d(TAG, "Updating expiration for: $pendingAppExitPackage")
                    authService.updateExpirationForAppExit(pendingAppExitPackage!!)
                } else {
                    Log.d(TAG, "Debounced: User returned to locked app, not updating expiration")
                }
                pendingAppExitPackage = null
            }
            handler.postDelayed(appExitRunnable!!, APP_EXIT_DELAY)
        }

        // Prompt logic (unchanged)
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
                    authService.requestAuthenticationIfNeeded(
                        AuthChannel.APPLOCK,
                        foregroundPackage,
                        object : AuthenticationCallback {
                            override fun onAuthenticationSucceeded(packageName: String) {
                                // No-op: handled by AppLockService UI/UX
                            }
                            override fun onAuthenticationFailed(packageName: String) {
                                // No-op: handled by AppLockService UI/UX
                            }
                        }
                    )
                }
            }
            handler.postDelayed(stablePromptRunnable!!, STABLE_DELAY)
        }

        lastPackageName = foregroundPackage
        lastClassName = foregroundClass
    }

    override fun onInterrupt() {}

    private fun isCerberusPromptActivity(className: String): Boolean {
        return cerberusPromptActivities.any { className.contains(it) }
    }

    private fun isProtectionEnabled(): Boolean {
        return ProtectionCache.isProtectionEnabled(this)
    }
}