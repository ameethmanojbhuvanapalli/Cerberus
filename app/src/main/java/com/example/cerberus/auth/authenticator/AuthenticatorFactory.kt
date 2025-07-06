package com.example.cerberus.auth.authenticator

import com.example.cerberus.auth.authenticator.impl.BiometricAuthenticator
import com.example.cerberus.auth.authenticator.impl.PasswordAuthenticator
import com.example.cerberus.auth.authenticator.impl.PatternAuthenticator
import com.example.cerberus.auth.authenticator.impl.PinAuthenticator

object AuthenticatorFactory {
    private val authenticators = mutableMapOf<AuthenticatorType, Authenticator>()

    init {
        // Register default authenticator(s)
        authenticators[AuthenticatorType.BIOMETRIC] = BiometricAuthenticator()
        authenticators[AuthenticatorType.PIN] = PinAuthenticator()
        authenticators[AuthenticatorType.PATTERN] = PatternAuthenticator()
        authenticators[AuthenticatorType.PASSWORD] = PasswordAuthenticator()
    }

    fun getAuthenticator(type: AuthenticatorType): Authenticator {
        return authenticators[type] ?: throw IllegalArgumentException("Authenticator type $type not registered")
    }

    fun registerAuthenticator(type: AuthenticatorType, authenticator: Authenticator) {
        authenticators[type] = authenticator
    }
}