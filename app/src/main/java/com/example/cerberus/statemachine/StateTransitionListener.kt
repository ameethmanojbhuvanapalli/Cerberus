package com.example.cerberus.statemachine

/**
 * Interface for listening to state transitions in the app lock state machine.
 * Implementations can react to state changes to perform appropriate actions
 * such as updating UI, triggering authentication, or logging events.
 */
interface StateTransitionListener {
    /**
     * Called when an app's state has changed.
     *
     * @param packageName The package name of the app that changed state
     * @param fromState The previous state of the app
     * @param toState The new state of the app
     * @param event The event that triggered this state transition
     */
    fun onStateChanged(
        packageName: String,
        fromState: AppState,
        toState: AppState,
        event: AppEvent
    )

    /**
     * Called when a state transition is attempted but rejected due to invalid transition.
     *
     * @param packageName The package name of the app
     * @param currentState The current state of the app
     * @param event The event that was attempted but rejected
     */
    fun onTransitionRejected(
        packageName: String,
        currentState: AppState,
        event: AppEvent
    )
}