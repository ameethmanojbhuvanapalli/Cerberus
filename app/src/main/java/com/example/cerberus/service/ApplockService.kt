package com.example.cerberus.service

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.cerberus.data.SharedPreferencesUtil
import com.example.cerberus.utils.BiometricAuthManager

class AppLockService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkIntervalMs = 1000L

    private val runnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, checkIntervalMs)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        handler.post(runnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Optional: can handle events here if needed
    }

    override fun onInterrupt() {
        handler.removeCallbacks(runnable)
    }

    private var lastLockedApp: String? = null

    private fun checkForegroundApp() {
        val foregroundApp = getForegroundAppPackageName()
        android.util.Log.d("AppLockService", "Foreground app: $foregroundApp")
        if (foregroundApp != null) {
            val lockedApps = SharedPreferencesUtil.getLockedApps(this)
            if (lockedApps.contains(foregroundApp) && foregroundApp != lastLockedApp) {
                lastLockedApp = foregroundApp
                launchLockScreen()
            } else if (!lockedApps.contains(foregroundApp)) {
                lastLockedApp = null
            }
        }
    }


    private fun launchLockScreen() {
        val intent = Intent(this, BiometricAuthManager::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        startActivity(intent)
    }

    private fun getForegroundAppPackageName(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 10 * 1000L

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            beginTime,
            endTime
        )

        if (usageStatsList.isNullOrEmpty()) return null

        return usageStatsList.maxByOrNull { it.lastTimeUsed }?.packageName
    }
}
