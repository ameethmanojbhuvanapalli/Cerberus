package com.example.cerberus.statemachine

/**
 * Represents the different states in the app lifecycle for lock management.
 * This enum defines the possible states that an app can be in within the
 * DFA-based state machine for enhanced app lock management.
 */
enum class AppState {
    /**
     * Initial state when no app is being tracked or all apps are idle
     */
    IDLE,

    /**
     * App is currently in the foreground and visible to user
     */
    FOREGROUND,

    /**
     * Authentication prompt is being shown to the user
     */
    AUTHENTICATING,

    /**
     * User has successfully authenticated and app access is granted
     */
    AUTHENTICATED,

    /**
     * App has been moved to background (user navigated away)
     */
    BACKGROUND,

    /**
     * App session has ended (timeout expired or explicitly closed)
     */
    EXITED
}