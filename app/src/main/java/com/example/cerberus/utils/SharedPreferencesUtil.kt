package com.example.cerberus.utils

import android.content.Context
import androidx.core.content.edit
import com.example.cerberus.auth.AuthenticatorType

object SharedPreferencesUtil {

    private const val PREFS_NAME = "AppLockPrefs"
    private const val LOCKED_APPS_KEY = "locked_apps"
    private const val AUTH_TYPE_KEY = "auth_type"
    private const val PROTECTION_ENABLED_KEY = "protection_enabled"
    private const val PIN_HASH_KEY = "pin_hash"
    private const val PATTERN_HASH_KEY = "pattern_hash"
    private const val PASSWORD_HASH_KEY = "password_hash"
    private const val IDLE_TIMEOUT_KEY = "idle_timeout_ms"
    private const val DEFAULT_IDLE_TIMEOUT = 5 * 60 * 1000L

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

    fun setPinHash(context: Context, pinHash: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(PIN_HASH_KEY, pinHash) }
    }

    fun getPinHash(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PIN_HASH_KEY, null)
    }

    fun clearPin(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(PIN_HASH_KEY) }
    }

    fun hasPin(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(PIN_HASH_KEY)
    }

    fun setPatternHash(context: Context, hash: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(PATTERN_HASH_KEY, hash) }
    }

    fun getPatternHash(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PATTERN_HASH_KEY, null)
    }

    fun hasPattern(context: Context): Boolean {
        return getPatternHash(context) != null
    }

    fun clearPattern(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { remove(PATTERN_HASH_KEY) }
    }

    fun setPasswordHash(context: Context, hash: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(PASSWORD_HASH_KEY, hash) }
    }
    fun getPasswordHash(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PASSWORD_HASH_KEY, null)
    fun clearPassword(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { remove(PASSWORD_HASH_KEY) }
    }
    fun hasPassword(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .contains(PASSWORD_HASH_KEY)

    fun getIdleTimeout(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(IDLE_TIMEOUT_KEY, DEFAULT_IDLE_TIMEOUT)
    }

    fun setIdleTimeout(context: Context, timeoutMs: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putLong(IDLE_TIMEOUT_KEY, timeoutMs) }
    }

    fun isProtectionEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PROTECTION_ENABLED_KEY, false)
    }

    fun setProtectionEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(PROTECTION_ENABLED_KEY, enabled) }
    }
}

