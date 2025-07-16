package com.example.cerberus.auth

import android.content.Context
import com.example.cerberus.auth.ModularAuthenticationService
import com.example.cerberus.auth.authenticator.Authenticator
import com.example.cerberus.auth.authenticator.AuthenticatorChangeListener
import com.example.cerberus.auth.authenticator.AuthenticatorFactory
import com.example.cerberus.auth.authenticator.AuthenticatorType
import com.example.cerberus.data.AuthenticatorTypeCache
import com.example.cerberus.data.IdleTimeoutCache
import java.lang.ref.WeakReference

class AuthenticationManager private constructor(context: Context) {
    private val contextRef = WeakReference(context.applicationContext)
    private var currentAuthenticator: Authenticator? = null
    private val listeners = mutableListOf<AuthenticatorChangeListener>()

    private val _authService: ModularAuthenticationService by lazy {
        ModularAuthenticationService(contextRef.get()!!, getCurrentAuthenticator())
    }

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
        _authService.updateAuthenticator(newAuthenticator)
    }

    fun registerListener(listener: AuthenticatorChangeListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun setIdleTimeout(context: Context, timeout: Long) {
        IdleTimeoutCache.setIdleTimeout(context, timeout)
        _authService.clearAuthenticatedApps()
    }

    private fun notifyListeners() {
        currentAuthenticator?.let { auth ->
            listeners.forEach { it.onAuthenticatorChanged(auth) }
        }
    }

    fun getAuthService(): ModularAuthenticationService = _authService

    fun shutdown() {
        _authService.shutdown()
         instance = null
    }
}