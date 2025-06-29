package com.example.cerberus.data

import android.content.Context

object IdleTimeoutCache {

    private var cachedTimeout: Long? = null

    fun getIdleTimeout(context: Context): Long {
        if (cachedTimeout == null) {
            cachedTimeout = SharedPreferencesUtil.getIdleTimeout(context)
        }
        return cachedTimeout ?: 15000L
    }

    fun setIdleTimeout(context: Context, timeoutMs: Long) {
        SharedPreferencesUtil.setIdleTimeout(context, timeoutMs)
        cachedTimeout = timeoutMs
    }

    fun refresh(context: Context) {
        cachedTimeout = SharedPreferencesUtil.getIdleTimeout(context)
    }

    fun clear() {
        cachedTimeout = null
    }
}
