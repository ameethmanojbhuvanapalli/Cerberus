package com.example.cerberus.auth

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

class AuthenticationStateMachineTest {
    
    private lateinit var stateMachine: AuthenticationStateMachine
    private val testPackage = "com.test.app"
    private val timeoutMs = 5000L
    
    @Before
    fun setUp() {
        stateMachine = AuthenticationStateMachine()
    }
    
    @Test
    fun initialState_shouldBeUnauthenticated() {
        assertEquals(
            AuthenticationStateMachine.State.UNAUTHENTICATED,
            stateMachine.getCurrentState(testPackage)
        )
        assertFalse(stateMachine.isAuthenticated(testPackage, timeoutMs))
        assertFalse(stateMachine.isPromptActive(testPackage))
    }
    
    @Test
    fun promptRequested_fromUnauthenticated_shouldTransitionToPrompting() {
        val event = AuthenticationStateMachine.Event.PromptRequested(testPackage)
        
        val transitionOccurred = stateMachine.processEvent(event)
        
        assertTrue(transitionOccurred)
        assertEquals(
            AuthenticationStateMachine.State.PROMPTING,
            stateMachine.getCurrentState(testPackage)
        )
        assertTrue(stateMachine.isPromptActive(testPackage))
        assertFalse(stateMachine.isAuthenticated(testPackage, timeoutMs))
    }
    
    @Test
    fun authenticationSucceeded_fromPrompting_shouldTransitionToAuthenticated() {
        // First transition to PROMPTING
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptRequested(testPackage))
        
        val event = AuthenticationStateMachine.Event.AuthenticationSucceeded(testPackage)
        val transitionOccurred = stateMachine.processEvent(event)
        
        assertTrue(transitionOccurred)
        assertEquals(
            AuthenticationStateMachine.State.AUTHENTICATED,
            stateMachine.getCurrentState(testPackage)
        )
        assertTrue(stateMachine.isAuthenticated(testPackage, timeoutMs))
        assertFalse(stateMachine.isPromptActive(testPackage))
    }
    
    @Test
    fun authenticationFailed_fromPrompting_shouldStayInPrompting() {
        // First transition to PROMPTING
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptRequested(testPackage))
        
        val event = AuthenticationStateMachine.Event.AuthenticationFailed(testPackage)
        val transitionOccurred = stateMachine.processEvent(event)
        
        assertFalse(transitionOccurred) // Should not transition
        assertEquals(
            AuthenticationStateMachine.State.PROMPTING,
            stateMachine.getCurrentState(testPackage)
        )
        assertTrue(stateMachine.isPromptActive(testPackage))
        assertFalse(stateMachine.isAuthenticated(testPackage, timeoutMs))
    }
    
    @Test
    fun promptDestroyed_fromPrompting_shouldTransitionToUnauthenticated() {
        // First transition to PROMPTING
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptRequested(testPackage))
        
        val event = AuthenticationStateMachine.Event.PromptDestroyed(testPackage)
        val transitionOccurred = stateMachine.processEvent(event)
        
        assertTrue(transitionOccurred)
        assertEquals(
            AuthenticationStateMachine.State.UNAUTHENTICATED,
            stateMachine.getCurrentState(testPackage)
        )
        assertFalse(stateMachine.isPromptActive(testPackage))
        assertFalse(stateMachine.isAuthenticated(testPackage, timeoutMs))
    }
    
    @Test
    fun appExited_fromAuthenticated_shouldTransitionToUnauthenticated() {
        // First transition to AUTHENTICATED
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptRequested(testPackage))
        stateMachine.processEvent(AuthenticationStateMachine.Event.AuthenticationSucceeded(testPackage))
        
        val event = AuthenticationStateMachine.Event.AppExited(testPackage)
        val transitionOccurred = stateMachine.processEvent(event)
        
        assertTrue(transitionOccurred)
        assertEquals(
            AuthenticationStateMachine.State.UNAUTHENTICATED,
            stateMachine.getCurrentState(testPackage)
        )
        assertFalse(stateMachine.isAuthenticated(testPackage, timeoutMs))
    }
    
    @Test
    fun setAuthenticationTimeout_shouldRespectTimeout() {
        // First transition to AUTHENTICATED
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptRequested(testPackage))
        stateMachine.processEvent(AuthenticationStateMachine.Event.AuthenticationSucceeded(testPackage))
        
        // Set a short timeout
        stateMachine.setAuthenticationTimeout(testPackage, 100L)
        
        // Should still be authenticated immediately
        assertTrue(stateMachine.isAuthenticated(testPackage, 1000L))
        
        // Wait for timeout to expire
        Thread.sleep(150L)
        
        // Should no longer be authenticated
        assertFalse(stateMachine.isAuthenticated(testPackage, 1000L))
    }
    
    @Test
    fun multiplePackages_shouldHaveIndependentStates() {
        val package1 = "com.test.app1"
        val package2 = "com.test.app2"
        
        // Package 1: UNAUTHENTICATED → PROMPTING
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptRequested(package1))
        
        // Package 2: UNAUTHENTICATED → PROMPTING → AUTHENTICATED
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptRequested(package2))
        stateMachine.processEvent(AuthenticationStateMachine.Event.AuthenticationSucceeded(package2))
        
        // Verify independent states
        assertEquals(AuthenticationStateMachine.State.PROMPTING, stateMachine.getCurrentState(package1))
        assertEquals(AuthenticationStateMachine.State.AUTHENTICATED, stateMachine.getCurrentState(package2))
        assertTrue(stateMachine.isPromptActive(package1))
        assertFalse(stateMachine.isPromptActive(package2))
        assertFalse(stateMachine.isAuthenticated(package1, timeoutMs))
        assertTrue(stateMachine.isAuthenticated(package2, timeoutMs))
    }
    
    @Test
    fun clearAllStates_shouldResetAllPackages() {
        val package1 = "com.test.app1"
        val package2 = "com.test.app2"
        
        // Set up different states for different packages
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptRequested(package1))
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptRequested(package2))
        stateMachine.processEvent(AuthenticationStateMachine.Event.AuthenticationSucceeded(package2))
        
        stateMachine.clearAllStates()
        
        // All should be back to UNAUTHENTICATED
        assertEquals(AuthenticationStateMachine.State.UNAUTHENTICATED, stateMachine.getCurrentState(package1))
        assertEquals(AuthenticationStateMachine.State.UNAUTHENTICATED, stateMachine.getCurrentState(package2))
        assertFalse(stateMachine.isAuthenticated(package1, timeoutMs))
        assertFalse(stateMachine.isAuthenticated(package2, timeoutMs))
    }
    
    @Test
    fun stateChangeListeners_shouldBeNotified() {
        var listenerCalled = false
        var capturedPackage = ""
        var capturedOldState: AuthenticationStateMachine.State? = null
        var capturedNewState: AuthenticationStateMachine.State? = null
        
        stateMachine.addStateChangeListener { packageName, oldState, newState ->
            listenerCalled = true
            capturedPackage = packageName
            capturedOldState = oldState
            capturedNewState = newState
        }
        
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptRequested(testPackage))
        
        assertTrue(listenerCalled)
        assertEquals(testPackage, capturedPackage)
        assertEquals(AuthenticationStateMachine.State.UNAUTHENTICATED, capturedOldState)
        assertEquals(AuthenticationStateMachine.State.PROMPTING, capturedNewState)
    }
    
    @Test
    fun completeAuthFlow_shouldFollowDFA() {
        var stateTransitions = mutableListOf<AuthenticationStateMachine.State>()
        
        stateMachine.addStateChangeListener { _, _, newState ->
            stateTransitions.add(newState)
        }
        
        // Complete successful authentication flow
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptRequested(testPackage))
        stateMachine.processEvent(AuthenticationStateMachine.Event.AuthenticationSucceeded(testPackage))
        stateMachine.processEvent(AuthenticationStateMachine.Event.AppExited(testPackage))
        
        assertEquals(
            listOf(
                AuthenticationStateMachine.State.PROMPTING,
                AuthenticationStateMachine.State.AUTHENTICATED,
                AuthenticationStateMachine.State.UNAUTHENTICATED
            ),
            stateTransitions
        )
    }
    
    @Test
    fun authenticationFailureAndPromptDestroy_shouldFollowDFA() {
        var stateTransitions = mutableListOf<AuthenticationStateMachine.State>()
        
        stateMachine.addStateChangeListener { _, _, newState ->
            stateTransitions.add(newState)
        }
        
        // Authentication failure followed by prompt destruction
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptRequested(testPackage))
        stateMachine.processEvent(AuthenticationStateMachine.Event.AuthenticationFailed(testPackage))
        stateMachine.processEvent(AuthenticationStateMachine.Event.PromptDestroyed(testPackage))
        
        assertEquals(
            listOf(
                AuthenticationStateMachine.State.PROMPTING, // Only 2 transitions, auth failure doesn't change state
                AuthenticationStateMachine.State.UNAUTHENTICATED
            ),
            stateTransitions
        )
    }
}