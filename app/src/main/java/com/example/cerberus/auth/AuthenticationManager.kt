package com.example.cerberus.auth

import android.content.Context
import com.example.cerberus.data.AuthenticatorTypeCache

class AuthenticationManager(private val context: Context) {
    private var currentAuthenticator: Authenticator? = null

    companion object {
        private var instance: AuthenticationManager? = null

        @Synchronized
        fun getInstance(context: Context): AuthenticationManager {
            if (instance == null) {
                instance = AuthenticationManager(context.applicationContext)
            }
            return instance!!
        }
    }

    fun getCurrentAuthenticator(): Authenticator {
        if (currentAuthenticator == null) {
            val type = AuthenticatorTypeCache.getAuthenticatorType(context)
            currentAuthenticator = AuthenticatorFactory.getAuthenticator(type)
        }
        return currentAuthenticator!!
    }

    fun setAuthenticatorType(type: AuthenticatorType) {
        AuthenticatorTypeCache.setAuthenticatorType(context, type)
        currentAuthenticator = AuthenticatorFactory.getAuthenticator(type)
    }
}