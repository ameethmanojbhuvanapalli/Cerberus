package com.example.cerberus.data

import android.content.Context

object PinCache {
    private var cachedPinHash: String? = null

    fun getPinHash(context: Context): String? {
        if (cachedPinHash == null) {
            cachedPinHash = SharedPreferencesUtil.getPinHash(context)
        }
        return cachedPinHash
    }

    fun setPin(context: Context, pinHash: String) {
        SharedPreferencesUtil.setPinHash(context, pinHash)
        cachedPinHash = pinHash
    }

    fun clear(context: Context) {
        SharedPreferencesUtil.clearPin(context)
        cachedPinHash = null
    }

    fun hasPin(context: Context): Boolean {
        return getPinHash(context) != null
    }
}