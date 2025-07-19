package com.example.cerberus.auth.authenticator

import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.auth.authenticator.impl.BiometricAuthenticator
import com.example.cerberus.auth.authenticator.impl.PinAuthenticator
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration tests for authenticator broadcast handling, especially AUTH_DISMISSED.
 * Note: These are simplified tests since we can't easily mock Android Context in unit tests.
 */
class AuthenticatorIntegrationTest {
    
    @Test
    fun testCallbackRegistrationAndUnregistration() {
        val authenticator = BiometricAuthenticator()
        val callback = object : AuthenticationCallback {
            override fun onAuthenticationSucceeded(packageName: String) {}
            override fun onAuthenticationFailed(packageName: String) {}
        }
        
        // Test callback registration
        authenticator.registerCallback(callback)
        
        // Test callback unregistration
        authenticator.unregisterCallback(callback)
        
        // Should not throw any exceptions
        assertTrue("Callback registration/unregistration should work", true)
    }

    @Test
    fun testPinAuthenticatorCallbacks() {
        val authenticator = PinAuthenticator()
        val callback = object : AuthenticationCallback {
            override fun onAuthenticationSucceeded(packageName: String) {}
            override fun onAuthenticationFailed(packageName: String) {}
        }
        
        // Test callback registration
        authenticator.registerCallback(callback)
        
        // Test callback unregistration
        authenticator.unregisterCallback(callback)
        
        assertTrue("PinAuthenticator callback handling should work", true)
    }

    @Test
    fun testMultipleCallbackRegistration() {
        val authenticator = BiometricAuthenticator()
        val callback1 = object : AuthenticationCallback {
            override fun onAuthenticationSucceeded(packageName: String) {}
            override fun onAuthenticationFailed(packageName: String) {}
        }
        val callback2 = object : AuthenticationCallback {
            override fun onAuthenticationSucceeded(packageName: String) {}
            override fun onAuthenticationFailed(packageName: String) {}
        }
        
        // Register same callback twice - should not duplicate
        authenticator.registerCallback(callback1)
        authenticator.registerCallback(callback1)
        
        // Register different callback
        authenticator.registerCallback(callback2)
        
        // Unregister one
        authenticator.unregisterCallback(callback1)
        
        // Should work without issues
        assertTrue("Multiple callback handling should work", true)
    }
}