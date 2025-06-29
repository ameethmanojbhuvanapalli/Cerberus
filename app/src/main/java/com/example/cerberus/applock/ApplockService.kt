package com.example.cerberus.applock

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
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

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.cerberus.STOP_APPLOCK") {
                AuthenticationManager.getInstance(applicationContext).stopAuthService()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        myPackageName = packageName

        val filter = IntentFilter("com.example.cerberus.STOP_APPLOCK")
        ContextCompat.registerReceiver(
            this,
            stopReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        AuthenticationManager.getInstance(applicationContext).startAuthService()
        AuthenticationManager.getInstance(applicationContext).getAuthService()?.cleanupExpiredEntries()
        Log.d(TAG, "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isProtectionEnabled()) return
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundPackage = event.packageName?.toString() ?: return
        val foregroundClass = event.className?.toString() ?: return

        if (foregroundPackage == myPackageName && foregroundClass.contains(promptActivityName)) return

        val lockedApps = LockedAppsCache.getLockedApps(this).toMutableSet().apply { add(myPackageName) }

        if (lastPackageName != null && lastPackageName != foregroundPackage) {
            if (lockedApps.contains(lastPackageName)) {
                AuthenticationManager.getInstance(applicationContext).getAuthService()
                    ?.updateExpirationForAppExit(lastPackageName!!)
            }
        }

        if (lockedApps.contains(foregroundPackage)) {
            if (lastPackageName != foregroundPackage) {
                activityChangeCount = 1
                stableSince = System.currentTimeMillis()
            } else if (lastClassName != foregroundClass) {
                activityChangeCount++
                stableSince = System.currentTimeMillis()
            }

            stablePromptRunnable?.let { handler.removeCallbacks(it) }
            stablePromptRunnable = Runnable {
                if (lockedApps.contains(foregroundPackage)
                    && lastPackageName == foregroundPackage
                    && lastClassName == foregroundClass
                ) {
                    Log.d(TAG, "Prompting after $activityChangeCount activity changes and ${System.currentTimeMillis() - stableSince}ms dwell")
                    AuthenticationManager.getInstance(applicationContext).getAuthService()
                        ?.requestAuthenticationIfNeeded(foregroundPackage)
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

    override fun onDestroy() {
        unregisterReceiver(stopReceiver)
        lastPackageName?.let {
            AuthenticationManager.getInstance(applicationContext).getAuthService()
                ?.updateExpirationForAppExit(it)
        }
        super.onDestroy()
    }
}
