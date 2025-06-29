package com.example.cerberus.applock

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

object ApplockManager {
    fun start(context: Context) {
        if (!isServiceEnabled(context)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun stop(context: Context) {
        val intent = Intent("com.example.cerberus.STOP_APPLOCK")
        context.sendBroadcast(intent)
    }

    private fun isServiceEnabled(context: Context): Boolean {
        val expectedComponent = "${context.packageName}/${AppLockService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return !TextUtils.isEmpty(enabledServices) && enabledServices!!.contains(expectedComponent)
    }
}
