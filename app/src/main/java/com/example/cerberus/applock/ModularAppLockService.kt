package com.example.cerberus.applock

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.cerberus.auth.AuthChannel
import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.auth.AuthenticationManager
import com.example.cerberus.auth.listeners.AppLifecycleListener
import com.example.cerberus.data.LockedAppsCache
import com.example.cerberus.data.ProtectionCache
import com.example.cerberus.lifecycle.AppLifecycleDetector

/**
 * Refactored AppLockService using modular components.
 * Uses AppLifecycleDetector for clean lifecycle management.
 */
class ModularAppLockService : AccessibilityService() {
    private var lastPackageName: String? = null
    private var lastClassName: String? = null
    private val TAG = "ModularAppLockService"
    private lateinit var myPackageName: String
    
    private val handler = Handler(Looper.getMainLooper())
    private var stablePromptRunnable: Runnable? = null
    private var stableSince: Long = 0L
    private var activityChangeCount: Int = 0
    private val STABLE_DELAY = 500L
    
    private lateinit var lifecycleDetector: AppLifecycleDetector

    private val authService
        get() = AuthenticationManager.getInstance(applicationContext).getAuthService()

    override fun onServiceConnected() {
        super.onServiceConnected()
        myPackageName = packageName
        
        // Initialize lifecycle detector
        lifecycleDetector = AppLifecycleDetector(myPackageName)
        
        // Register lifecycle listener to handle app exits
        lifecycleDetector.registerListener(object : AppLifecycleListener {
            override fun onAppEntered(packageName: String) {
                Log.d(TAG, "Lifecycle: App entered - $packageName")
            }

            override fun onAppExited(packageName: String) {
                Log.d(TAG, "Lifecycle: App exited - $packageName")
                authService.updateExpirationForAppExit(packageName)
            }

            override fun onPromptActivityDetected(packageName: String, className: String) {
                Log.d(TAG, "Lifecycle: Prompt activity detected - $className")
            }
        })
        
        Log.d(TAG, "Modular service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isProtectionEnabled()) return
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundPackage = event.packageName?.toString() ?: return
        val foregroundClass = event.className?.toString() ?: return

        val lockedApps = LockedAppsCache.getLockedApps(this).toSet()

        // Use lifecycle detector to process the event
        lifecycleDetector.onWindowStateChanged(foregroundPackage, foregroundClass, lockedApps)
        
        // Handle prompt logic for locked apps
        handlePromptLogic(foregroundPackage, foregroundClass, lockedApps)

        lastPackageName = foregroundPackage
        lastClassName = foregroundClass
    }

    private fun handlePromptLogic(
        foregroundPackage: String,
        foregroundClass: String,
        lockedApps: Set<String>
    ) {
        // Skip if system package
        if (lifecycleDetector.isSystemPackage(foregroundPackage)) {
            return
        }

        // Skip if Cerberus prompt activity
        if (lifecycleDetector.isCerberusPrompt(foregroundPackage, foregroundClass)) {
            return
        }

        // Check if we need to prompt for authentication
        if (lockedApps.contains(foregroundPackage) &&
            lastPackageName != null &&
            lastPackageName != foregroundPackage &&
            !authService.isAuthenticated(foregroundPackage)
        ) {
            
            activityChangeCount = 1
            stableSince = System.currentTimeMillis()

            stablePromptRunnable?.let { handler.removeCallbacks(it) }
            stablePromptRunnable = Runnable {
                // Only prompt if still in the same package/class as when scheduled
                if (lockedApps.contains(foregroundPackage) &&
                    lastPackageName == foregroundPackage &&
                    lastClassName == foregroundClass
                ) {
                    Log.d(TAG, "Prompting after $activityChangeCount activity changes and ${System.currentTimeMillis() - stableSince}ms dwell")
                    authService.requestAuthenticationIfNeeded(
                        AuthChannel.APPLOCK,
                        foregroundPackage,
                        object : AuthenticationCallback {
                            override fun onAuthenticationSucceeded(packageName: String) {
                                Log.d(TAG, "Authentication succeeded for $packageName")
                            }
                            override fun onAuthenticationFailed(packageName: String) {
                                Log.d(TAG, "Authentication failed for $packageName")
                            }
                        }
                    )
                }
            }
            handler.postDelayed(stablePromptRunnable!!, STABLE_DELAY)
        }
    }

    override fun onInterrupt() {}

    private fun isProtectionEnabled(): Boolean {
        return ProtectionCache.isProtectionEnabled(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::lifecycleDetector.isInitialized) {
            lifecycleDetector.shutdown()
        }
        stablePromptRunnable?.let { handler.removeCallbacks(it) }
        Log.d(TAG, "Modular service destroyed")
    }
}