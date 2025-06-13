package com.example.cerberus.data

import android.content.Context
import androidx.core.content.edit

object SharedPreferencesUtil {

    private const val PREFS_NAME = "AppLockPrefs"
    private const val LOCKED_APPS_KEY = "locked_apps"

    fun getLockedApps(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(LOCKED_APPS_KEY, emptySet()) ?: emptySet()
    }

    fun setLockedApps(context: Context, apps: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putStringSet(LOCKED_APPS_KEY, apps) }
    }
}
