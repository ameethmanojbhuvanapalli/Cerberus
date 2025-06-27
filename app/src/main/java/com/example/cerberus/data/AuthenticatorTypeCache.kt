package com.example.cerberus.data

import android.content.Context
import com.example.cerberus.auth.AuthenticatorType

object AuthenticatorTypeCache {

    private var cachedAuthenticatorType: AuthenticatorType? = null

    fun getAuthenticatorType(context: Context): AuthenticatorType {
        if (cachedAuthenticatorType == null) {
            cachedAuthenticatorType = SharedPreferencesUtil.getAuthenticatorType(context)
        }
        return cachedAuthenticatorType ?: AuthenticatorType.BIOMETRIC
    }

    fun setAuthenticatorType(context: Context, type: AuthenticatorType) {
        SharedPreferencesUtil.setAuthenticatorType(context, type)
        cachedAuthenticatorType = type
    }

    fun refresh(context: Context) {
        cachedAuthenticatorType = SharedPreferencesUtil.getAuthenticatorType(context)
    }

    fun clear() {
        cachedAuthenticatorType = null
    }
}