package com.example.cerberus.applock.statemachine

/**
 * Represents the different states of the app lock service.
 * 
 * The state machine ensures that only one authentication prompt can be shown
 * at a time and prevents race conditions between app switches.
 */
enum class LockState {
    /**
     * Default state - no authentication needed or in progress.
     * The service is monitoring for locked app access but not actively prompting.
     */
    IDLE,
    
    /**
     * A locked app has been detected but we're waiting for the app to stabilize.
     * This settlement period handles app startup timing and gesture navigation.
     */
    LOCKED_APP_DETECTED,
    
    /**
     * Authentication prompt is currently being shown to the user.
     * No additional prompts should be triggered while in this state.
     */
    PROMPTING,
    
    /**
     * User has successfully authenticated for the current app.
     * The app remains accessible until the user leaves it.
     */
    AUTHENTICATED
}