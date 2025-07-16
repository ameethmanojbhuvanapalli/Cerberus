package com.example.cerberus.applock.statemachine

/**
 * Sealed class representing all possible events that can trigger state transitions
 * in the app lock service state machine.
 */
sealed class LockEvent {
    
    /**
     * A locked app has been opened by the user.
     * @param packageName The package name of the locked app
     * @param className The activity class name 
     */
    data class LockedAppOpened(
        val packageName: String,
        val className: String
    ) : LockEvent()
    
    /**
     * A non-locked app has been opened by the user.
     * @param packageName The package name of the app
     */
    data class NonLockedAppOpened(
        val packageName: String
    ) : LockEvent()
    
    /**
     * The same app activity has changed (navigation within app).
     * This should not trigger authentication.
     * @param packageName The package name of the app
     * @param className The new activity class name
     */
    data class SameAppActivityChanged(
        val packageName: String,
        val className: String
    ) : LockEvent()
    
    /**
     * The settlement period for a locked app has completed.
     * Ready to show authentication prompt if still needed.
     */
    data object SettlementCompleted : LockEvent()
    
    /**
     * Authentication has been successfully completed for an app.
     * @param packageName The package name that was authenticated
     */
    data class AuthenticationSucceeded(
        val packageName: String
    ) : LockEvent()
    
    /**
     * Authentication failed or was cancelled by the user.
     * @param packageName The package name that failed authentication
     */
    data class AuthenticationFailed(
        val packageName: String
    ) : LockEvent()
    
    /**
     * User has left the authenticated app, starting timeout period.
     * @param packageName The package name of the app that was left
     */
    data class AppLeft(
        val packageName: String
    ) : LockEvent()
    
    /**
     * A system package or gesture animation was detected.
     * These should be ignored to prevent false triggers.
     * @param packageName The system package name
     */
    data class SystemPackageDetected(
        val packageName: String
    ) : LockEvent()
    
    /**
     * Cerberus app itself was opened.
     * Should never require authentication.
     */
    data object CerberusAppOpened : LockEvent()
    
    /**
     * Reset the state machine to idle state.
     * Used for error recovery or service restart.
     */
    data object Reset : LockEvent()
}