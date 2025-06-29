package com.example.cerberus.auth

import com.example.cerberus.auth.impl.BiometricAuthenticator
import com.example.cerberus.auth.impl.PatternAuthenticator
import com.example.cerberus.auth.impl.PinAuthenticator

object AuthenticatorFactory {
    private val authenticators = mutableMapOf<AuthenticatorType, Authenticator>()

    init {
        // Register default authenticator(s)
        authenticators[AuthenticatorType.BIOMETRIC] = BiometricAuthenticator()
        authenticators[AuthenticatorType.PIN] = PinAuthenticator()
        authenticators[AuthenticatorType.PATTERN] = PatternAuthenticator()
    }

    fun getAuthenticator(type: AuthenticatorType): Authenticator {
        return authenticators[type] ?: throw IllegalArgumentException("Authenticator type $type not registered")
    }

    fun registerAuthenticator(type: AuthenticatorType, authenticator: Authenticator) {
        authenticators[type] = authenticator
    }
}