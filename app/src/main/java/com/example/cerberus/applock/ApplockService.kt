package com.example.cerberus.applock

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.cerberus.data.ProtectionCache
import com.example.cerberus.statemachine.AppStateMachine

/**
 * AppLockService - Pure AccessibilityService focused on event detection
 * 
 * This service follows Single Responsibility Principle:
 * - Only handles accessibility event detection and filtering
 * - Delegates all event processing to EventProcessor
 * - No authentication logic (handled by EventProcessor + AuthenticationService)
 * - Clean separation of concerns
 */
class AppLockService : AccessibilityService() {
    private val TAG = "AppLockService"
    private lateinit var myPackageName: String
    private val promptActivityName = "com.example.cerberus.utils.BiometricPromptActivity"

    // Enhanced state management components
    private val stateMachine = AppStateMachine()
    private lateinit var eventProcessor: EventProcessor

    override fun onServiceConnected() {
        super.onServiceConnected()
        myPackageName = packageName
        
        // Initialize enhanced event processor with authentication integration
        eventProcessor = EventProcessor(applicationContext, stateMachine, myPackageName, promptActivityName)
        
        Log.d(TAG, "AppLockService connected - delegating to EventProcessor for enhanced processing")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isProtectionEnabled()) return
        
        // Delegate all event processing to EventProcessor
        eventProcessor.processAccessibilityEvent(event)
    }

    override fun onInterrupt() {
        Log.d(TAG, "AppLockService interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Cleanup event processor
        if (::eventProcessor.isInitialized) {
            eventProcessor.shutdown()
        }
        
        // Clear state machine
        stateMachine.clearAllStates()
        
        Log.d(TAG, "AppLockService destroyed and cleaned up")
    }

    private fun isProtectionEnabled(): Boolean {
        return ProtectionCache.isProtectionEnabled(this)
    }
}