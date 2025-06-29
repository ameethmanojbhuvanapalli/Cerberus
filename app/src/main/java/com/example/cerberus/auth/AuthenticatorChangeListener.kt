package com.example.cerberus.auth

interface AuthenticatorChangeListener {
    fun onAuthenticatorChanged(newAuthenticator: Authenticator)
}
