package com.example.cerberus.auth.state

import org.junit.Test
import org.junit.Assert.*
import com.example.cerberus.auth.listeners.AuthStateListener

/**
 * Unit tests for AuthenticationStateMachine
 */
class AuthenticationStateMachineTest {

    @Test
    fun testInitialState() {
        val stateMachine = AuthenticationStateMachine()
        val state = stateMachine.getState("com.example.app")
        
        assertEquals("com.example.app", state.packageName)
        assertEquals(AuthState.UNAUTHENTICATED, state.state)
        assertFalse(state.isAuthenticated())
    }

    @Test
    fun testValidStateTransitions() {
        val stateMachine = AuthenticationStateMachine()
        val packageName = "com.example.app"
        
        // UNAUTHENTICATED -> PROMPTING
        val promptingState = stateMachine.transitionTo(packageName, AuthState.PROMPTING)
        assertEquals(AuthState.PROMPTING, promptingState.state)
        
        // PROMPTING -> AUTHENTICATED
        val authenticatedState = stateMachine.transitionTo(packageName, AuthState.AUTHENTICATED, Long.MAX_VALUE)
        assertEquals(AuthState.AUTHENTICATED, authenticatedState.state)
        assertTrue(authenticatedState.isAuthenticated())
        
        // AUTHENTICATED -> UNAUTHENTICATED
        val unauthenticatedState = stateMachine.transitionTo(packageName, AuthState.UNAUTHENTICATED)
        assertEquals(AuthState.UNAUTHENTICATED, unauthenticatedState.state)
        assertFalse(unauthenticatedState.isAuthenticated())
    }

    @Test
    fun testInvalidStateTransitions() {
        val stateMachine = AuthenticationStateMachine()
        val packageName = "com.example.app"
        
        // Try invalid transition: UNAUTHENTICATED -> AUTHENTICATED (should fail)
        val invalidState = stateMachine.transitionTo(packageName, AuthState.AUTHENTICATED)
        assertEquals(AuthState.UNAUTHENTICATED, invalidState.state) // Should remain unchanged
    }

    @Test
    fun testExpirationHandling() {
        val stateMachine = AuthenticationStateMachine()
        val packageName = "com.example.app"
        
        // Set authenticated with past expiration
        val pastTime = System.currentTimeMillis() - 1000
        val expiredState = stateMachine.transitionTo(packageName, AuthState.PROMPTING)
        val authenticatedState = stateMachine.transitionTo(packageName, AuthState.AUTHENTICATED, pastTime)
        
        assertTrue(authenticatedState.isExpired())
        assertFalse(authenticatedState.isAuthenticated())
    }

    @Test
    fun testStateListener() {
        val stateMachine = AuthenticationStateMachine()
        val packageName = "com.example.app"
        var notificationCount = 0
        var lastOldState: AuthenticationState? = null
        var lastNewState: AuthenticationState? = null
        
        val listener = object : AuthStateListener {
            override fun onStateChanged(oldState: AuthenticationState, newState: AuthenticationState) {
                notificationCount++
                lastOldState = oldState
                lastNewState = newState
            }
        }
        
        stateMachine.registerListener(listener)
        
        // Trigger a state change
        stateMachine.transitionTo(packageName, AuthState.PROMPTING)
        
        assertEquals(1, notificationCount)
        assertEquals(AuthState.UNAUTHENTICATED, lastOldState?.state)
        assertEquals(AuthState.PROMPTING, lastNewState?.state)
    }

    @Test
    fun testClearAllStates() {
        val stateMachine = AuthenticationStateMachine()
        
        // Set up multiple authenticated apps
        stateMachine.transitionTo("app1", AuthState.PROMPTING)
        stateMachine.transitionTo("app1", AuthState.AUTHENTICATED, Long.MAX_VALUE)
        stateMachine.transitionTo("app2", AuthState.PROMPTING)
        stateMachine.transitionTo("app2", AuthState.AUTHENTICATED, Long.MAX_VALUE)
        
        // Verify they exist
        assertTrue(stateMachine.getState("app1").isAuthenticated())
        assertTrue(stateMachine.getState("app2").isAuthenticated())
        
        // Clear all
        stateMachine.clearAllStates()
        
        // Verify they're cleared
        assertFalse(stateMachine.getState("app1").isAuthenticated())
        assertFalse(stateMachine.getState("app2").isAuthenticated())
    }

    @Test
    fun testThreadSafety() {
        val stateMachine = AuthenticationStateMachine()
        val packageName = "com.example.app"
        
        // This is a basic test - in a real scenario you'd use multiple threads
        // For now, just verify multiple rapid operations don't cause issues
        repeat(100) { i ->
            stateMachine.transitionTo(packageName, AuthState.PROMPTING)
            stateMachine.transitionTo(packageName, AuthState.AUTHENTICATED, Long.MAX_VALUE)
            stateMachine.transitionTo(packageName, AuthState.UNAUTHENTICATED)
        }
        
        val finalState = stateMachine.getState(packageName)
        assertEquals(AuthState.UNAUTHENTICATED, finalState.state)
    }
}