package com.example.cerberus.data

import android.content.Context
import androidx.core.content.edit
import com.example.cerberus.auth.AuthenticatorType

object SharedPreferencesUtil {

    private const val PREFS_NAME = "AppLockPrefs"
    private const val LOCKED_APPS_KEY = "locked_apps"
    private const val AUTH_TYPE_KEY = "auth_type"

    fun getLockedApps(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(LOCKED_APPS_KEY, emptySet()) ?: emptySet()
    }

    fun setLockedApps(context: Context, apps: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putStringSet(LOCKED_APPS_KEY, apps) }
    }

    fun getAuthenticatorType(context: Context): AuthenticatorType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val typeName = prefs.getString(AUTH_TYPE_KEY, AuthenticatorType.BIOMETRIC.name)
        return try {
            AuthenticatorType.valueOf(typeName ?: AuthenticatorType.BIOMETRIC.name)
        } catch (e: Exception) {
            // Fallback if the stored value is invalid
            AuthenticatorType.BIOMETRIC
        }
    }

    fun setAuthenticatorType(context: Context, type: AuthenticatorType) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(AUTH_TYPE_KEY, type.name) }
    }
}