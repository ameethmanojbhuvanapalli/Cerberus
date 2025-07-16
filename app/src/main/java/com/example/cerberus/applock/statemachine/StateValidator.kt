package com.example.cerberus.applock.statemachine

import android.util.Log

/**
 * Validates state transitions in the app lock state machine to prevent invalid
 * transitions and ensure the state machine maintains consistency.
 */
object StateValidator {
    
    private val TAG = "StateValidator"
    
    /**
     * Validates if a state transition is allowed.
     * 
     * @param currentState The current state of the state machine
     * @param event The event that would trigger the transition
     * @param targetState The target state that would result from the transition
     * @return true if the transition is valid, false if it should be blocked
     */
    fun isValidTransition(
        currentState: LockState,
        event: LockEvent,
        targetState: LockState
    ): Boolean {
        val isValid = when (currentState) {
            LockState.IDLE -> validateFromIdle(event, targetState)
            LockState.LOCKED_APP_DETECTED -> validateFromLockedAppDetected(event, targetState)
            LockState.PROMPTING -> validateFromPrompting(event, targetState)
            LockState.AUTHENTICATED -> validateFromAuthenticated(event, targetState)
        }
        
        if (!isValid) {
            Log.w(TAG, "Invalid transition blocked: $currentState -> $targetState via $event")
        } else {
            Log.d(TAG, "Valid transition: $currentState -> $targetState via $event")
        }
        
        return isValid
    }
    
    /**
     * Validates transitions from IDLE state
     */
    private fun validateFromIdle(event: LockEvent, targetState: LockState): Boolean {
        return when (event) {
            is LockEvent.LockedAppOpened -> targetState == LockState.LOCKED_APP_DETECTED
            is LockEvent.NonLockedAppOpened -> targetState == LockState.IDLE
            is LockEvent.CerberusAppOpened -> targetState == LockState.IDLE
            is LockEvent.SystemPackageDetected -> targetState == LockState.IDLE
            is LockEvent.SameAppActivityChanged -> targetState == LockState.IDLE
            is LockEvent.Reset -> targetState == LockState.IDLE
            // These events should not occur in IDLE state
            is LockEvent.SettlementCompleted,
            is LockEvent.AuthenticationSucceeded,
            is LockEvent.AuthenticationFailed,
            is LockEvent.AppLeft -> false
        }
    }
    
    /**
     * Validates transitions from LOCKED_APP_DETECTED state
     */
    private fun validateFromLockedAppDetected(event: LockEvent, targetState: LockState): Boolean {
        return when (event) {
            is LockEvent.SettlementCompleted -> targetState == LockState.PROMPTING || targetState == LockState.AUTHENTICATED
            is LockEvent.NonLockedAppOpened -> targetState == LockState.IDLE
            is LockEvent.LockedAppOpened -> targetState == LockState.LOCKED_APP_DETECTED
            is LockEvent.CerberusAppOpened -> targetState == LockState.IDLE
            is LockEvent.SystemPackageDetected -> targetState == LockState.LOCKED_APP_DETECTED
            is LockEvent.SameAppActivityChanged -> targetState == LockState.LOCKED_APP_DETECTED
            is LockEvent.Reset -> targetState == LockState.IDLE
            // These events should not occur in LOCKED_APP_DETECTED state
            is LockEvent.AuthenticationSucceeded,
            is LockEvent.AuthenticationFailed,
            is LockEvent.AppLeft -> false
        }
    }
    
    /**
     * Validates transitions from PROMPTING state
     */
    private fun validateFromPrompting(event: LockEvent, targetState: LockState): Boolean {
        return when (event) {
            is LockEvent.AuthenticationSucceeded -> targetState == LockState.AUTHENTICATED
            is LockEvent.AuthenticationFailed -> targetState == LockState.IDLE
            is LockEvent.NonLockedAppOpened -> targetState == LockState.IDLE
            is LockEvent.CerberusAppOpened -> targetState == LockState.IDLE
            is LockEvent.Reset -> targetState == LockState.IDLE
            // These events should be ignored while prompting to prevent interruption
            is LockEvent.LockedAppOpened,
            is LockEvent.SystemPackageDetected,
            is LockEvent.SameAppActivityChanged,
            is LockEvent.SettlementCompleted,
            is LockEvent.AppLeft -> targetState == LockState.PROMPTING // Stay in same state
        }
    }
    
    /**
     * Validates transitions from AUTHENTICATED state
     */
    private fun validateFromAuthenticated(event: LockEvent, targetState: LockState): Boolean {
        return when (event) {
            is LockEvent.AppLeft -> targetState == LockState.IDLE
            is LockEvent.NonLockedAppOpened -> targetState == LockState.IDLE
            is LockEvent.LockedAppOpened -> targetState == LockState.LOCKED_APP_DETECTED
            is LockEvent.CerberusAppOpened -> targetState == LockState.IDLE
            is LockEvent.SameAppActivityChanged -> targetState == LockState.AUTHENTICATED
            is LockEvent.SystemPackageDetected -> targetState == LockState.AUTHENTICATED
            is LockEvent.Reset -> targetState == LockState.IDLE
            // These events should not occur in AUTHENTICATED state
            is LockEvent.SettlementCompleted,
            is LockEvent.AuthenticationSucceeded,
            is LockEvent.AuthenticationFailed -> false
        }
    }
    
    /**
     * Checks if an event should be ignored in the current state.
     * Some events are valid but should not trigger any state change.
     */
    fun shouldIgnoreEvent(currentState: LockState, event: LockEvent): Boolean {
        return when (currentState) {
            LockState.PROMPTING -> {
                // Ignore most events while prompting to prevent interruption
                when (event) {
                    is LockEvent.LockedAppOpened,
                    is LockEvent.SystemPackageDetected,
                    is LockEvent.SameAppActivityChanged,
                    is LockEvent.SettlementCompleted,
                    is LockEvent.AppLeft -> true
                    else -> false
                }
            }
            else -> {
                // System package events are always ignored
                event is LockEvent.SystemPackageDetected
            }
        }
    }
    
    /**
     * Provides a human-readable explanation for why a transition was invalid.
     * Useful for debugging and logging.
     */
    fun getTransitionErrorReason(
        currentState: LockState,
        event: LockEvent,
        targetState: LockState
    ): String {
        return when (currentState) {
            LockState.IDLE -> when (event) {
                is LockEvent.SettlementCompleted -> "Settlement should not complete in IDLE state"
                is LockEvent.AuthenticationSucceeded -> "Authentication cannot succeed without prompting"
                is LockEvent.AuthenticationFailed -> "Authentication cannot fail without prompting"
                is LockEvent.AppLeft -> "Cannot leave app in IDLE state"
                else -> "Unexpected event $event in IDLE state"
            }
            LockState.LOCKED_APP_DETECTED -> when (event) {
                is LockEvent.AuthenticationSucceeded -> "Authentication cannot succeed without prompting"
                is LockEvent.AuthenticationFailed -> "Authentication cannot fail without prompting"
                is LockEvent.AppLeft -> "Cannot leave app during settlement period"
                else -> "Unexpected event $event in LOCKED_APP_DETECTED state"
            }
            LockState.PROMPTING -> when (event) {
                is LockEvent.SettlementCompleted -> "Settlement should not complete while prompting"
                else -> "Event $event should be ignored while prompting"
            }
            LockState.AUTHENTICATED -> when (event) {
                is LockEvent.SettlementCompleted -> "Settlement should not complete while authenticated"
                is LockEvent.AuthenticationSucceeded -> "Already authenticated"
                is LockEvent.AuthenticationFailed -> "Cannot fail authentication while authenticated"
                else -> "Unexpected event $event in AUTHENTICATED state"
            }
        }
    }
}