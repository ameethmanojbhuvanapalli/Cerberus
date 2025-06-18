package com.example.cerberus.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.example.cerberus.data.LockedAppsCache
import com.example.cerberus.utils.BiometricAuthManager

class AppLockService : AccessibilityService() {

    private var lastLockedApp: String? = null
    private var isAuthenticated = false

    private val authReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.cerberus.AUTH_SUCCESS") {
                isAuthenticated = true
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter("com.example.cerberus.AUTH_SUCCESS")
        registerReceiver(authReceiver, filter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val foregroundApp = event.packageName?.toString()
            if (foregroundApp == packageName || foregroundApp == BiometricAuthManager::class.java.name) {
                return
            }
            val lockedApps = LockedAppsCache.getLockedApps(this)
            if (foregroundApp != null) {
                if (lockedApps.contains(foregroundApp)) {
                    if (!isAuthenticated || foregroundApp != lastLockedApp) {
                        lastLockedApp = foregroundApp
                        isAuthenticated = false
                        launchLockScreen()
                    }
                } else {
                    lastLockedApp = null
                    isAuthenticated = false
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        unregisterReceiver(authReceiver)
        super.onDestroy()
    }

    private fun launchLockScreen() {
        val intent = Intent(this, BiometricAuthManager::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        startActivity(intent)
    }
}
