package com.example.cerberus.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.core.net.toUri
import com.example.cerberus.applock.AppLockService

object PermissionsUtil {

    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasAccessibilityPermission(context)
                && hasOverlayPermission(context)
    }

    fun hasOverlayPermission(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun requestOverlayPermission(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri())
        context.startActivity(intent)
    }

    fun hasAccessibilityPermission(context: Context): Boolean {
        val expectedServiceId = "${context.packageName}/${AppLockService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expectedServiceId, ignoreCase = true)) return true
        }
        return false
    }

    fun requestAccessibilityPermission(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
    }
}