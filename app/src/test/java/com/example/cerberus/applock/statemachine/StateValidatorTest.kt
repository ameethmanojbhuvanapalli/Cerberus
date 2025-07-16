package com.example.cerberus.applock.statemachine

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for StateValidator to ensure valid state transitions
 */
class StateValidatorTest {

    @Test
    fun testValidTransitionsFromIdle() {
        // Valid transitions from IDLE
        assertTrue("Locked app opened from IDLE should be valid", 
            StateValidator.isValidTransition(
                LockState.IDLE, 
                LockEvent.LockedAppOpened("com.example.app", "MainActivity"), 
                LockState.LOCKED_APP_DETECTED
            ))
        
        assertTrue("Non-locked app opened from IDLE should be valid", 
            StateValidator.isValidTransition(
                LockState.IDLE, 
                LockEvent.NonLockedAppOpened("com.example.app"), 
                LockState.IDLE
            ))
        
        assertTrue("Cerberus app opened from IDLE should be valid", 
            StateValidator.isValidTransition(
                LockState.IDLE, 
                LockEvent.CerberusAppOpened, 
                LockState.IDLE
            ))
    }

    @Test
    fun testInvalidTransitionsFromIdle() {
        // Invalid transitions from IDLE
        assertFalse("Settlement should not complete in IDLE", 
            StateValidator.isValidTransition(
                LockState.IDLE, 
                LockEvent.SettlementCompleted, 
                LockState.PROMPTING
            ))
        
        assertFalse("Authentication cannot succeed without prompting", 
            StateValidator.isValidTransition(
                LockState.IDLE, 
                LockEvent.AuthenticationSucceeded("com.example.app"), 
                LockState.AUTHENTICATED
            ))
        
        assertFalse("Cannot leave app in IDLE state", 
            StateValidator.isValidTransition(
                LockState.IDLE, 
                LockEvent.AppLeft("com.example.app"), 
                LockState.IDLE
            ))
    }

    @Test
    fun testValidTransitionsFromLockedAppDetected() {
        assertTrue("Settlement completion should transition to PROMPTING", 
            StateValidator.isValidTransition(
                LockState.LOCKED_APP_DETECTED, 
                LockEvent.SettlementCompleted, 
                LockState.PROMPTING
            ))
        
        assertTrue("Non-locked app should transition to IDLE", 
            StateValidator.isValidTransition(
                LockState.LOCKED_APP_DETECTED, 
                LockEvent.NonLockedAppOpened("com.example.app"), 
                LockState.IDLE
            ))
        
        assertTrue("Another locked app should stay in LOCKED_APP_DETECTED", 
            StateValidator.isValidTransition(
                LockState.LOCKED_APP_DETECTED, 
                LockEvent.LockedAppOpened("com.example.app2", "MainActivity"), 
                LockState.LOCKED_APP_DETECTED
            ))
    }

    @Test
    fun testValidTransitionsFromPrompting() {
        assertTrue("Auth success should transition to AUTHENTICATED", 
            StateValidator.isValidTransition(
                LockState.PROMPTING, 
                LockEvent.AuthenticationSucceeded("com.example.app"), 
                LockState.AUTHENTICATED
            ))
        
        assertTrue("Auth failure should transition to IDLE", 
            StateValidator.isValidTransition(
                LockState.PROMPTING, 
                LockEvent.AuthenticationFailed("com.example.app"), 
                LockState.IDLE
            ))
        
        assertTrue("Non-locked app should transition to IDLE", 
            StateValidator.isValidTransition(
                LockState.PROMPTING, 
                LockEvent.NonLockedAppOpened("com.example.app"), 
                LockState.IDLE
            ))
    }

    @Test
    fun testPromptingStateIgnoresEvents() {
        // Events that should be ignored while prompting
        assertTrue("Should stay in PROMPTING when locked app opened", 
            StateValidator.isValidTransition(
                LockState.PROMPTING, 
                LockEvent.LockedAppOpened("com.example.app", "MainActivity"), 
                LockState.PROMPTING
            ))
        
        assertTrue("Should stay in PROMPTING for system packages", 
            StateValidator.isValidTransition(
                LockState.PROMPTING, 
                LockEvent.SystemPackageDetected("com.android.systemui"), 
                LockState.PROMPTING
            ))
    }

    @Test
    fun testValidTransitionsFromAuthenticated() {
        assertTrue("App left should transition to IDLE", 
            StateValidator.isValidTransition(
                LockState.AUTHENTICATED, 
                LockEvent.AppLeft("com.example.app"), 
                LockState.IDLE
            ))
        
        assertTrue("Non-locked app should transition to IDLE", 
            StateValidator.isValidTransition(
                LockState.AUTHENTICATED, 
                LockEvent.NonLockedAppOpened("com.example.app"), 
                LockState.IDLE
            ))
        
        assertTrue("New locked app should transition to LOCKED_APP_DETECTED", 
            StateValidator.isValidTransition(
                LockState.AUTHENTICATED, 
                LockEvent.LockedAppOpened("com.example.app2", "MainActivity"), 
                LockState.LOCKED_APP_DETECTED
            ))
        
        assertTrue("Same app activity change should stay AUTHENTICATED", 
            StateValidator.isValidTransition(
                LockState.AUTHENTICATED, 
                LockEvent.SameAppActivityChanged("com.example.app", "SettingsActivity"), 
                LockState.AUTHENTICATED
            ))
    }

    @Test
    fun testResetTransitions() {
        // Reset should always work from any state
        LockState.values().forEach { state ->
            assertTrue("Reset should work from $state", 
                StateValidator.isValidTransition(
                    state, 
                    LockEvent.Reset, 
                    LockState.IDLE
                ))
        }
    }

    @Test
    fun testShouldIgnoreEvent() {
        assertTrue("Should ignore locked app opened while prompting", 
            StateValidator.shouldIgnoreEvent(
                LockState.PROMPTING, 
                LockEvent.LockedAppOpened("com.example.app", "MainActivity")
            ))
        
        assertTrue("Should ignore settlement while prompting", 
            StateValidator.shouldIgnoreEvent(
                LockState.PROMPTING, 
                LockEvent.SettlementCompleted
            ))
        
        assertTrue("Should always ignore system packages", 
            StateValidator.shouldIgnoreEvent(
                LockState.IDLE, 
                LockEvent.SystemPackageDetected("com.android.systemui")
            ))
        
        assertFalse("Should not ignore auth success while prompting", 
            StateValidator.shouldIgnoreEvent(
                LockState.PROMPTING, 
                LockEvent.AuthenticationSucceeded("com.example.app")
            ))
    }

    @Test
    fun testTransitionErrorReasons() {
        val reason = StateValidator.getTransitionErrorReason(
            LockState.IDLE,
            LockEvent.SettlementCompleted,
            LockState.PROMPTING
        )
        assertTrue("Should provide meaningful error reason", reason.isNotEmpty())
        assertTrue("Should mention settlement issue", reason.contains("settlement") || reason.contains("Settlement"))
    }
}