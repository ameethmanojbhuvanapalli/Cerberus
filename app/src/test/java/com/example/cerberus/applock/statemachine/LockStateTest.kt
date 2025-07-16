package com.example.cerberus.applock.statemachine

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for the LockState enum and state machine states
 */
class LockStateTest {

    @Test
    fun testAllStatesAreDefined() {
        val states = LockState.values()
        assertEquals("Should have exactly 4 states", 4, states.size)
        
        val expectedStates = setOf(
            LockState.IDLE,
            LockState.LOCKED_APP_DETECTED,
            LockState.PROMPTING,
            LockState.AUTHENTICATED
        )
        
        assertEquals("All expected states should be present", expectedStates, states.toSet())
    }
    
    @Test
    fun testStateEnumValues() {
        assertEquals(LockState.IDLE, LockState.valueOf("IDLE"))
        assertEquals(LockState.LOCKED_APP_DETECTED, LockState.valueOf("LOCKED_APP_DETECTED"))
        assertEquals(LockState.PROMPTING, LockState.valueOf("PROMPTING"))
        assertEquals(LockState.AUTHENTICATED, LockState.valueOf("AUTHENTICATED"))
    }
}