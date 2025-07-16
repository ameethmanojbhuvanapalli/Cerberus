package com.example.cerberus.auth.state

/**
 * Represents the authentication state for an app
 */
enum class AuthState {
    UNAUTHENTICATED,
    PROMPTING, 
    AUTHENTICATED
}

/**
 * Immutable authentication state data
 */
data class AuthenticationState(
    val packageName: String,
    val state: AuthState,
    val timestamp: Long = System.currentTimeMillis(),
    val expirationTime: Long? = null
) {
    fun isExpired(): Boolean {
        return expirationTime?.let { System.currentTimeMillis() > it } ?: false
    }
    
    fun isAuthenticated(): Boolean {
        return state == AuthState.AUTHENTICATED && !isExpired()
    }
}