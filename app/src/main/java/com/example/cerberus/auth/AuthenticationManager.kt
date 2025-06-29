package com.example.cerberus.auth

import android.content.Context
import com.example.cerberus.data.AuthenticatorTypeCache
import java.lang.ref.WeakReference

class AuthenticationManager private constructor(context: Context) {
    private val contextRef = WeakReference(context.applicationContext)
    private var currentAuthenticator: Authenticator? = null
    private val listeners = mutableListOf<AuthenticatorChangeListener>()
    private var authService: AuthenticationService? = null

    companion object {
        @Volatile
        private var instance: AuthenticationManager? = null

        @Synchronized
        fun getInstance(context: Context): AuthenticationManager {
            if (instance == null) {
                instance = AuthenticationManager(context)
            }
            return instance!!
        }
    }

    fun getCurrentAuthenticator(): Authenticator {
        if (currentAuthenticator == null) {
            val type = AuthenticatorTypeCache.getAuthenticatorType(contextRef.get()!!)
            currentAuthenticator = AuthenticatorFactory.getAuthenticator(type)
        }
        return currentAuthenticator!!
    }

    fun setAuthenticatorType(type: AuthenticatorType) {
        contextRef.get()?.let {
            AuthenticatorTypeCache.setAuthenticatorType(it, type)
        }
        val newAuthenticator = AuthenticatorFactory.getAuthenticator(type)
        currentAuthenticator = newAuthenticator
        notifyListeners()
        authService?.updateAuthenticator(newAuthenticator)
    }

    fun registerListener(listener: AuthenticatorChangeListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    private fun notifyListeners() {
        currentAuthenticator?.let { auth ->
            listeners.forEach { it.onAuthenticatorChanged(auth) }
        }
    }

    fun startAuthService() {
        if (authService == null) {
            contextRef.get()?.let {
                authService = AuthenticationService(it, getCurrentAuthenticator())
            }
        }
        authService?.cleanupExpiredEntries()
    }

    fun stopAuthService() {
        authService?.shutdown()
        authService = null
    }

    fun getAuthService(): AuthenticationService? = authService

    fun clearAuthenticatedApps() {
        authService?.clearAuthenticatedApps()
    }
}
