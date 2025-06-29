package com.example.cerberus.data

import android.content.Context

object IdleTimeoutCache {

    private var cachedTimeout: Long? = null

    fun getIdleTimeout(context: Context): Long {
        if (cachedTimeout == null) {
            cachedTimeout = SharedPreferencesUtil.getIdleTimeout(context)
        }
        return cachedTimeout!!
    }

    fun setIdleTimeout(context: Context, timeoutMs: Long) {
        SharedPreferencesUtil.setIdleTimeout(context, timeoutMs)
        cachedTimeout = timeoutMs
    }

    fun clear() {
        cachedTimeout = null
    }
}
