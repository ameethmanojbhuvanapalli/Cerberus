package com.example.cerberus.data

import android.content.Context
import com.example.cerberus.auth.AuthenticationManager
import com.example.cerberus.utils.SharedPreferencesUtil

object ProtectionCache {
    private var cached: Boolean? = null

    fun isProtectionEnabled(context: Context): Boolean {
        if (cached == null) {
            cached = SharedPreferencesUtil.isProtectionEnabled(context)
        }
        return cached!!
    }

    fun setProtectionEnabled(context: Context, enabled: Boolean) {
        if (!enabled) {
            AuthenticationManager.getInstance(context).shutdown()
        }
        SharedPreferencesUtil.setProtectionEnabled(context, enabled)
        cached = enabled
    }

    fun clear() {
        cached = null
    }
}
