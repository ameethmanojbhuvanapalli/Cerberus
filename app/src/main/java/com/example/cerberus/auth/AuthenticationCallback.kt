package com.example.cerberus.auth

interface AuthenticationCallback {
    /**
     * Called when authentication succeeded for a package
     * @param packageName The package name that was successfully authenticated
     */
    fun onAuthenticationSucceeded(packageName: String)

    /**
     * Called when authentication failed for a package
     * @param packageName The package name for which authentication failed
     */
    fun onAuthenticationFailed(packageName: String)
}