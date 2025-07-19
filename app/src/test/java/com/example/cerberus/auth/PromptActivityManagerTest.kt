package com.example.cerberus.auth

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Unit tests for PromptActivityManager to verify proper coordination and race condition prevention.
 */
class PromptActivityManagerTest {
    
    @Before
    fun setUp() {
        // Clear any existing state before each test
        PromptActivityManager.clearAllPrompts()
    }

    @Test
    fun testSinglePromptRegistration() {
        val packageName = "com.example.testapp"
        val promptType = "biometric"
        
        // First registration should succeed
        assertTrue(PromptActivityManager.registerPrompt(packageName, promptType))
        assertTrue(PromptActivityManager.isPromptActive(packageName))
        assertEquals(promptType, PromptActivityManager.getActivePromptType(packageName))
        assertNotNull(PromptActivityManager.getPromptLock(packageName))
    }

    @Test
    fun testMultiplePromptsPrevention() {
        val packageName = "com.example.testapp"
        
        // Register first prompt
        assertTrue(PromptActivityManager.registerPrompt(packageName, "biometric"))
        
        // Second prompt should be rejected
        assertFalse(PromptActivityManager.registerPrompt(packageName, "pin"))
        
        // Active prompt should still be the first one
        assertEquals("biometric", PromptActivityManager.getActivePromptType(packageName))
    }

    @Test
    fun testDifferentPackagesCanHavePrompts() {
        val packageName1 = "com.example.testapp1"
        val packageName2 = "com.example.testapp2"
        
        // Both packages should be able to register prompts
        assertTrue(PromptActivityManager.registerPrompt(packageName1, "biometric"))
        assertTrue(PromptActivityManager.registerPrompt(packageName2, "pin"))
        
        // Both should be active
        assertTrue(PromptActivityManager.isPromptActive(packageName1))
        assertTrue(PromptActivityManager.isPromptActive(packageName2))
        
        // Each should have their respective prompt types
        assertEquals("biometric", PromptActivityManager.getActivePromptType(packageName1))
        assertEquals("pin", PromptActivityManager.getActivePromptType(packageName2))
    }

    @Test
    fun testUnregisterPrompt() {
        val packageName = "com.example.testapp"
        val promptType = "pattern"
        
        // Register and verify
        assertTrue(PromptActivityManager.registerPrompt(packageName, promptType))
        assertTrue(PromptActivityManager.isPromptActive(packageName))
        
        // Unregister and verify
        PromptActivityManager.unregisterPrompt(packageName, promptType)
        assertFalse(PromptActivityManager.isPromptActive(packageName))
        assertNull(PromptActivityManager.getActivePromptType(packageName))
        assertNull(PromptActivityManager.getPromptLock(packageName))
    }

    @Test
    fun testUnregisterWrongPromptType() {
        val packageName = "com.example.testapp"
        
        // Register biometric prompt
        assertTrue(PromptActivityManager.registerPrompt(packageName, "biometric"))
        
        // Try to unregister with wrong type
        PromptActivityManager.unregisterPrompt(packageName, "pin")
        
        // Should still be active with original type
        assertTrue(PromptActivityManager.isPromptActive(packageName))
        assertEquals("biometric", PromptActivityManager.getActivePromptType(packageName))
    }

    @Test
    fun testClearAllPrompts() {
        val packageName1 = "com.example.testapp1"
        val packageName2 = "com.example.testapp2"
        
        // Register multiple prompts
        assertTrue(PromptActivityManager.registerPrompt(packageName1, "biometric"))
        assertTrue(PromptActivityManager.registerPrompt(packageName2, "pin"))
        
        // Clear all
        PromptActivityManager.clearAllPrompts()
        
        // All should be inactive
        assertFalse(PromptActivityManager.isPromptActive(packageName1))
        assertFalse(PromptActivityManager.isPromptActive(packageName2))
        assertNull(PromptActivityManager.getActivePromptType(packageName1))
        assertNull(PromptActivityManager.getActivePromptType(packageName2))
    }

    @Test
    fun testReregisterAfterUnregister() {
        val packageName = "com.example.testapp"
        
        // Register, unregister, then register again with different type
        assertTrue(PromptActivityManager.registerPrompt(packageName, "biometric"))
        PromptActivityManager.unregisterPrompt(packageName, "biometric")
        assertTrue(PromptActivityManager.registerPrompt(packageName, "pin"))
        
        // Should now have the new prompt type
        assertEquals("pin", PromptActivityManager.getActivePromptType(packageName))
    }
}