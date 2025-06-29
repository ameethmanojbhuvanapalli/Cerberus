package com.example.cerberus.data

import android.content.Context

object PasswordCache {
    private var cachedPasswordHash: String? = null

    fun getPasswordHash(context: Context): String? {
        if (cachedPasswordHash == null) cachedPasswordHash = SharedPreferencesUtil.getPasswordHash(context)
        return cachedPasswordHash
    }

    fun setPassword(context: Context, hash: String) {
        SharedPreferencesUtil.setPasswordHash(context, hash)
        cachedPasswordHash = hash
    }

    fun clear(context: Context) {
        SharedPreferencesUtil.clearPassword(context)
        cachedPasswordHash = null
    }

    fun hasPassword(context: Context): Boolean = getPasswordHash(context) != null
}
