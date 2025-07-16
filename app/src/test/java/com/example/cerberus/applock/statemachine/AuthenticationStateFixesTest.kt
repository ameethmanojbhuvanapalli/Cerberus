package com.example.cerberus.applock.statemachine

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for the authentication state machine fixes to ensure proper handling
 * of already-authenticated apps and gesture navigation events.
 */
class AuthenticationStateFixesTest {

    @Test
    fun testSettlementCompletedToAuthenticatedTransition() {
        // Test that LOCKED_APP_DETECTED -> AUTHENTICATED is now a valid transition
        // This fixes the invalid transition issue where already-authenticated apps
        // were incorrectly transitioning to IDLE instead of AUTHENTICATED
        assertTrue("Settlement completion should allow transition to AUTHENTICATED for already-authenticated apps", 
            StateValidator.isValidTransition(
                LockState.LOCKED_APP_DETECTED, 
                LockEvent.SettlementCompleted, 
                LockState.AUTHENTICATED
            ))
    }

    @Test
    fun testSettlementCompletedToPromptingStillValid() {
        // Ensure existing behavior for non-authenticated apps is preserved
        assertTrue("Settlement completion should still allow transition to PROMPTING for non-authenticated apps", 
            StateValidator.isValidTransition(
                LockState.LOCKED_APP_DETECTED, 
                LockEvent.SettlementCompleted, 
                LockState.PROMPTING
            ))
    }

    @Test
    fun testVivoGesturePackagesFiltered() {
        // Test that Vivo gesture navigation packages are properly filtered
        assertTrue("com.vivo.upslide should be detected as system package", 
            SystemPackageFilter.isSystemPackage("com.vivo.upslide"))
        
        assertTrue("com.vivo.gesture should be detected as system package", 
            SystemPackageFilter.isSystemPackage("com.vivo.gesture"))
        
        assertTrue("com.bbk.gesture should be detected as system package", 
            SystemPackageFilter.isSystemPackage("com.bbk.gesture"))
    }

    @Test
    fun testEnhancedGestureAnimationFiltering() {
        // Test enhanced gesture animation detection
        assertTrue("launcher3 overview should be detected as gesture animation", 
            SystemPackageFilter.isGestureAnimation("com.android.launcher3.overview"))
        
        assertTrue("launcher3 taskbar should be detected as gesture animation", 
            SystemPackageFilter.isGestureAnimation("com.android.launcher3.taskbar"))
        
        assertTrue("Samsung gestural should be detected as gesture animation", 
            SystemPackageFilter.isGestureAnimation("com.samsung.android.gestural"))
        
        assertTrue("OnePlus gesture should be detected as gesture animation", 
            SystemPackageFilter.isGestureAnimation("com.oneplus.gesture"))
        
        assertTrue("MIUI gesture should be detected as gesture animation", 
            SystemPackageFilter.isGestureAnimation("com.miui.gesture"))
        
        assertTrue("Oppo gesture should be detected as gesture animation", 
            SystemPackageFilter.isGestureAnimation("com.oppo.gesture"))
        
        assertTrue("Huawei gesture should be detected as gesture animation", 
            SystemPackageFilter.isGestureAnimation("com.huawei.gesture"))
    }

    @Test
    fun testInvalidTransitionsStillBlocked() {
        // Ensure that invalid transitions are still properly blocked
        assertFalse("Invalid transitions should still be blocked", 
            StateValidator.isValidTransition(
                LockState.IDLE, 
                LockEvent.SettlementCompleted, 
                LockState.AUTHENTICATED
            ))
        
        assertFalse("Authentication events should not occur in LOCKED_APP_DETECTED", 
            StateValidator.isValidTransition(
                LockState.LOCKED_APP_DETECTED, 
                LockEvent.AuthenticationSucceeded("com.example.app"), 
                LockState.AUTHENTICATED
            ))
    }

    @Test
    fun testGesturePackagesPatternsDetection() {
        // Test that gesture-related patterns are detected properly
        assertTrue("Package containing 'gesture' should be detected", 
            SystemPackageFilter.isGestureAnimation("com.test.gesture.service"))
        
        assertTrue("Package containing 'transition' should be detected", 
            SystemPackageFilter.isGestureAnimation("com.test.transition.helper"))
        
        assertTrue("Package containing 'animation' should be detected", 
            SystemPackageFilter.isGestureAnimation("com.test.animation.controller"))
    }

    @Test 
    fun testVivoSystemPackagePatternMatching() {
        // Test pattern-based matching for Vivo packages
        assertTrue("Vivo gesture subpackage should be detected", 
            SystemPackageFilter.isSystemPackage("com.vivo.gesture.controller"))
        
        assertTrue("BBK gesture subpackage should be detected", 
            SystemPackageFilter.isSystemPackage("com.bbk.gesture.navigation"))
    }

    @Test
    fun testUserAppNotAffected() {
        // Ensure regular user apps are not affected by the fixes
        assertFalse("Regular user app should not be detected as system package", 
            SystemPackageFilter.isSystemPackage("com.instagram.android"))
        
        assertFalse("Regular user app should not be detected as gesture animation", 
            SystemPackageFilter.isGestureAnimation("com.facebook.katana"))
    }
}