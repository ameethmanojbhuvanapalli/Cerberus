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
    private val promptActivityName = "com.example.cerberus.utils.BiometricPromptActivity"
    private val systemPackages = setOf("com.android.systemui", "android", null)

    private val handler = Handler(Looper.getMainLooper())
    
    // Simplified app exit detection - no more complex debouncing
    private var appExitRunnable: Runnable? = null
    private val APP_EXIT_DELAY = 1000L // Reduced delay for cleaner detection
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

        // Don't process if prompt activity is being shown
        if (foregroundPackage == myPackageName && foregroundClass.contains(promptActivityName)) {
            lastPackageName = foregroundPackage
            lastClassName = foregroundClass
            return
        }

        val lockedApps = LockedAppsCache.getLockedApps(this).toMutableSet().apply { add(myPackageName) }

        // Handle app exit - simplified logic using state machine
        handleAppExit(lockedApps, foregroundPackage)

        // Handle authentication prompting - simplified logic
        handleAuthenticationPrompt(lockedApps, foregroundPackage)

        lastPackageName = foregroundPackage
        lastClassName = foregroundClass
    }
    
    private fun handleAppExit(lockedApps: Set<String>, foregroundPackage: String) {
        if (lastPackageName != null && 
            lastPackageName != foregroundPackage && 
            lockedApps.contains(lastPackageName)) {
            
            // User appears to have left a locked app
            pendingAppExitPackage = lastPackageName
            
            // Cancel any previous pending exit
            appExitRunnable?.let { handler.removeCallbacks(it) }
            
            appExitRunnable = Runnable {
                // Only update expiration if user did NOT return to the locked app
                if (pendingAppExitPackage != foregroundPackage) {
                    Log.d(TAG, "True app exit detected for: $pendingAppExitPackage")
                    authService.updateExpirationForAppExit(pendingAppExitPackage!!)
                } else {
                    Log.d(TAG, "User returned to locked app, not updating expiration")
                }
                pendingAppExitPackage = null
            }
            
            handler.postDelayed(appExitRunnable!!, APP_EXIT_DELAY)
        }
    }
    
    private fun handleAuthenticationPrompt(lockedApps: Set<String>, foregroundPackage: String) {
        // Check if we should prompt for this package
        if (lockedApps.contains(foregroundPackage) && 
            lastPackageName != null &&
            lastPackageName != foregroundPackage &&
            !authService.isAuthenticated(foregroundPackage)) {
            
            Log.d(TAG, "Requesting authentication for: $foregroundPackage")
            
            // Use state machine-based authentication request
            authService.requestAuthenticationIfNeeded(
                AuthChannel.APPLOCK,
                foregroundPackage,
                object : AuthenticationCallback {
                    override fun onAuthenticationSucceeded(packageName: String) {
                        Log.d(TAG, "Authentication succeeded for: $packageName")
                        // No additional action needed - handled by service
                    }
                    
                    override fun onAuthenticationFailed(packageName: String) {
                        Log.d(TAG, "Authentication failed for: $packageName")
                        // No additional action needed - handled by service
                    }
                }
            )
        }
    }

    override fun onInterrupt() {}

    private fun isProtectionEnabled(): Boolean {
        return ProtectionCache.isProtectionEnabled(this)
    }
}