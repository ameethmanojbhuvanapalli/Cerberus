package com.example.cerberus.statemachine

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for AppStateMachine DFA implementation
 */
class AppStateMachineTest {
    
    private lateinit var stateMachine: AppStateMachine
    private val testPackageName = "com.example.testapp"
    
    @Before
    fun setUp() {
        stateMachine = AppStateMachine()
    }
    
    @Test
    fun testInitialState() {
        val initialState = stateMachine.getCurrentState(testPackageName)
        assertEquals(AppState.IDLE, initialState)
    }
    
    @Test
    fun testValidTransitions() {
        // Test IDLE -> FOREGROUND
        assertTrue(stateMachine.processEvent(testPackageName, AppEvent.APP_LAUNCHED))
        assertEquals(AppState.FOREGROUND, stateMachine.getCurrentState(testPackageName))
        
        // Test FOREGROUND -> AUTHENTICATING
        assertTrue(stateMachine.processEvent(testPackageName, AppEvent.AUTH_PROMPT_SHOWN))
        assertEquals(AppState.AUTHENTICATING, stateMachine.getCurrentState(testPackageName))
        
        // Test AUTHENTICATING -> AUTHENTICATED
        assertTrue(stateMachine.processEvent(testPackageName, AppEvent.AUTH_SUCCESS))
        assertEquals(AppState.AUTHENTICATED, stateMachine.getCurrentState(testPackageName))
        
        // Test AUTHENTICATED -> BACKGROUND
        assertTrue(stateMachine.processEvent(testPackageName, AppEvent.BACKGROUND_GESTURE))
        assertEquals(AppState.BACKGROUND, stateMachine.getCurrentState(testPackageName))
        
        // Test BACKGROUND -> FOREGROUND (app return)
        assertTrue(stateMachine.processEvent(testPackageName, AppEvent.APP_RETURNED))
        assertEquals(AppState.FOREGROUND, stateMachine.getCurrentState(testPackageName))
    }
    
    @Test
    fun testInvalidTransitions() {
        // Try to authenticate from IDLE (should fail)
        assertFalse(stateMachine.processEvent(testPackageName, AppEvent.AUTH_SUCCESS))
        assertEquals(AppState.IDLE, stateMachine.getCurrentState(testPackageName))
        
        // Launch app to FOREGROUND
        assertTrue(stateMachine.processEvent(testPackageName, AppEvent.APP_LAUNCHED))
        
        // Try invalid transition from FOREGROUND
        assertFalse(stateMachine.processEvent(testPackageName, AppEvent.APP_RETURNED))
        assertEquals(AppState.FOREGROUND, stateMachine.getCurrentState(testPackageName))
    }
    
    @Test
    fun testAuthenticationFailure() {
        // Launch app and show auth prompt
        stateMachine.processEvent(testPackageName, AppEvent.APP_LAUNCHED)
        stateMachine.processEvent(testPackageName, AppEvent.AUTH_PROMPT_SHOWN)
        assertEquals(AppState.AUTHENTICATING, stateMachine.getCurrentState(testPackageName))
        
        // Authentication fails - should return to FOREGROUND
        assertTrue(stateMachine.processEvent(testPackageName, AppEvent.AUTH_FAILED))
        assertEquals(AppState.FOREGROUND, stateMachine.getCurrentState(testPackageName))
    }
    
    @Test
    fun testAppExit() {
        // Launch and authenticate app
        stateMachine.processEvent(testPackageName, AppEvent.APP_LAUNCHED)
        stateMachine.processEvent(testPackageName, AppEvent.AUTH_PROMPT_SHOWN)
        stateMachine.processEvent(testPackageName, AppEvent.AUTH_SUCCESS)
        
        // App exits - should clean up state
        assertTrue(stateMachine.processEvent(testPackageName, AppEvent.APP_CLOSED))
        assertEquals(AppState.IDLE, stateMachine.getCurrentState(testPackageName))
    }
    
    @Test
    fun testTimeoutExpiration() {
        // Launch, authenticate, and background the app
        stateMachine.processEvent(testPackageName, AppEvent.APP_LAUNCHED)
        stateMachine.processEvent(testPackageName, AppEvent.AUTH_PROMPT_SHOWN)
        stateMachine.processEvent(testPackageName, AppEvent.AUTH_SUCCESS)
        stateMachine.processEvent(testPackageName, AppEvent.BACKGROUND_GESTURE)
        assertEquals(AppState.BACKGROUND, stateMachine.getCurrentState(testPackageName))
        
        // Timeout expires
        assertTrue(stateMachine.processEvent(testPackageName, AppEvent.TIMEOUT_EXPIRED))
        assertEquals(AppState.IDLE, stateMachine.getCurrentState(testPackageName))
    }
    
    @Test
    fun testTransitionValidation() {
        assertTrue(stateMachine.isValidTransition(AppState.IDLE, AppEvent.APP_LAUNCHED))
        assertTrue(stateMachine.isValidTransition(AppState.FOREGROUND, AppEvent.AUTH_PROMPT_SHOWN))
        assertTrue(stateMachine.isValidTransition(AppState.AUTHENTICATING, AppEvent.AUTH_SUCCESS))
        
        assertFalse(stateMachine.isValidTransition(AppState.IDLE, AppEvent.AUTH_SUCCESS))
        assertFalse(stateMachine.isValidTransition(AppState.AUTHENTICATED, AppEvent.APP_LAUNCHED))
    }
    
    @Test
    fun testMultipleApps() {
        val app1 = "com.example.app1"
        val app2 = "com.example.app2"
        
        // Launch both apps
        stateMachine.processEvent(app1, AppEvent.APP_LAUNCHED)
        stateMachine.processEvent(app2, AppEvent.APP_LAUNCHED)
        
        assertEquals(AppState.FOREGROUND, stateMachine.getCurrentState(app1))
        assertEquals(AppState.FOREGROUND, stateMachine.getCurrentState(app2))
        
        // Authenticate app1
        stateMachine.processEvent(app1, AppEvent.AUTH_PROMPT_SHOWN)
        stateMachine.processEvent(app1, AppEvent.AUTH_SUCCESS)
        
        // States should be independent
        assertEquals(AppState.AUTHENTICATED, stateMachine.getCurrentState(app1))
        assertEquals(AppState.FOREGROUND, stateMachine.getCurrentState(app2))
    }
}