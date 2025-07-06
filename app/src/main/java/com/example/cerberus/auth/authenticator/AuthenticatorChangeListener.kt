package com.example.cerberus.auth.authenticator

interface AuthenticatorChangeListener {
    fun onAuthenticatorChanged(newAuthenticator: Authenticator)
}
