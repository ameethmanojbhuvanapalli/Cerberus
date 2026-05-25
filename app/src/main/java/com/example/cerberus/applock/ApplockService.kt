package com.example.cerberus.applock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
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
    private val TAG = "AppLockService"
    private var lastPackageName: String? = null
    private var lastClassName: String? = null
    private lateinit var myPackageName: String

    // Keep stable delay logic
    private val handler = Handler(Looper.getMainLooper())
    private var stablePromptRunnable: Runnable? = null
    private val STABLE_DELAY = 500L

    // Cache of launcher packages for performance
    private val launcherPackages: MutableSet<String> = mutableSetOf()

    private val authService
        get() = AuthenticationManager.getInstance(applicationContext).getAuthService()

    override fun onServiceConnected() {
        super.onServiceConnected()
        myPackageName = packageName
        findLauncherPackages()
        Log.d(TAG, "Service connected, discovered launchers: $launcherPackages")
    }

    /**
     * Identifies all installed launcher applications by checking which apps
     * can respond to the HOME intent
     */
    private fun findLauncherPackages() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }

        val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        for (resolveInfo in resolveInfos) {
            resolveInfo.activityInfo.packageName?.let {
                launcherPackages.add(it)
            }
        }

        // Fallback for common launchers in case the detection misses some
        launcherPackages.addAll(COMMON_LAUNCHER_PACKAGES)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isProtectionEnabled()) return
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundPackage = event.packageName?.toString() ?: return
        val foregroundClass = event.className?.toString() ?: return

        // Skip our own package
        if (foregroundPackage == myPackageName) return

        val lockedApps = LockedAppsCache.getLockedApps(this)

        // Handle launcher appearance - update expiration time for previous app
        if (isLauncherPackage(foregroundPackage) && lastPackageName != null && !isLauncherPackage(lastPackageName)) {
            if (lockedApps.contains(lastPackageName)) {
                Log.d(TAG, "Launcher detected, updating expiration for: $lastPackageName")
                authService.updateExpirationForAppExit(lastPackageName!!)
            }
        }

        // Handle locked app authentication
        if (lockedApps.contains(foregroundPackage) && !authService.isAuthenticated(foregroundPackage)) {
            // Cancel any previous authentication request
            stablePromptRunnable?.let { handler.removeCallbacks(it) }

            // Set up new authentication request with delay
            stablePromptRunnable = Runnable {
                // Only prompt if still in the same app
                if (lastPackageName == foregroundPackage) {
                    Log.d(TAG, "Requesting authentication for locked app: $foregroundPackage")
                    authService.requestAuthenticationIfNeeded(
                        AuthChannel.APPLOCK,
                        foregroundPackage,
                        object : AuthenticationCallback {
                            override fun onAuthenticationSucceeded(packageName: String) {
                                // Handled by authentication system
                            }
                            override fun onAuthenticationFailed(packageName: String) {
                                // Handled by authentication system
                            }
                        }
                    )
                }
            }
            handler.postDelayed(stablePromptRunnable!!, STABLE_DELAY)
        }

        // Update tracking variables
        lastPackageName = foregroundPackage
        lastClassName = foregroundClass
    }

    /**
     * Checks if the given package is a launcher app
     */
    private fun isLauncherPackage(packageName: String?): Boolean {
        if (packageName == null) return false
        return launcherPackages.contains(packageName)
    }

    override fun onInterrupt() {}

    private fun isProtectionEnabled(): Boolean {
        return ProtectionCache.isProtectionEnabled(this)
    }

    companion object {
        // Fallback list of common launcher package names across different manufacturers
        private val COMMON_LAUNCHER_PACKAGES = setOf(
            //"com.android.launcher3",              // AOSP
            "com.google.android.apps.nexuslauncher", // Pixel
            "com.sec.android.app.launcher",       // Samsung
            "com.sec.android.app.twlauncher",     // Older Samsung
            "com.miui.home",                      // Xiaomi
            "com.huawei.android.launcher",        // Huawei
            "net.oneplus.launcher",               // OnePlus
            "com.asus.launcher",                  // Asus
            "com.htc.launcher",                   // HTC
            "com.oppo.launcher",                  // Oppo
            "com.vivo.launcher"                   // Vivo
        )
    }
}