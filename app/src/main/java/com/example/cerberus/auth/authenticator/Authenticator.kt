package com.example.cerberus.auth.authenticator

import android.content.Context
import com.example.cerberus.auth.AuthenticationCallback

interface Authenticator {
    /**
     * Start the authentication process for the given package
     * @param context Application context used to launch authentication UI
     * @param packageName The package name of the app requiring authentication
     */
    fun authenticate(context: Context, packageName: String)

    /**
     * Register a callback to receive authentication results
     * @param callback The callback to receive authentication events
     */
    fun registerCallback(callback: AuthenticationCallback)

    /**
     * Unregister a previously registered callback
     * @param callback The callback to unregister
     */
    fun unregisterCallback(callback: AuthenticationCallback)
}