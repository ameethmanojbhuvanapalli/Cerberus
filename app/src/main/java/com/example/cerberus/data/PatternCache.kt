package com.example.cerberus.data

import android.content.Context

object PatternCache {
    private var cached: String? = null

    fun getPatternHash(context: Context): String? {
        if (cached == null) cached = SharedPreferencesUtil.getPatternHash(context)
        return cached
    }

    fun setPattern(context: Context, patternHash: String) {
        SharedPreferencesUtil.setPatternHash(context, patternHash)
        cached = patternHash
    }

    fun clear(context: Context) {
        SharedPreferencesUtil.clearPattern(context)
        cached = null
    }

    fun hasPattern(context: Context): Boolean {
        return getPatternHash(context) != null
    }
}
