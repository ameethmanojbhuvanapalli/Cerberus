package com.example.cerberus.applock

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.cerberus.applock.statemachine.AppLockStateMachine
import com.example.cerberus.applock.statemachine.EventProcessor
import com.example.cerberus.applock.statemachine.LockEvent
import com.example.cerberus.data.ProtectionCache

class AppLockService : AccessibilityService() {
    private val TAG = "AppLockService"
    private lateinit var myPackageName: String
    
    // State machine components
    private lateinit var stateMachine: AppLockStateMachine
    private lateinit var eventProcessor: EventProcessor
    
    // Last package name for event processing
    private var lastPackageName: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        myPackageName = packageName
        
        // Initialize state machine components
        stateMachine = AppLockStateMachine(applicationContext)
        eventProcessor = EventProcessor(applicationContext, myPackageName)
        
        Log.d(TAG, "Service connected with state machine initialized")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Check if protection is enabled
        if (!isProtectionEnabled()) return
        
        // Process the event through the event processor
        val lockEvent = eventProcessor.processEvent(event, lastPackageName)
        
        // Update last package name for next event
        event?.packageName?.toString()?.let { packageName ->
            lastPackageName = packageName
        }
        
        // Process the lock event through the state machine if valid
        lockEvent?.let { 
            val processed = stateMachine.processEvent(it)
            if (processed) {
                Log.d(TAG, "Event processed by state machine: $it")
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
        // Clean up state machine on interruption
        if (this::stateMachine.isInitialized) {
            stateMachine.shutdown()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        // Clean up state machine on destroy
        if (this::stateMachine.isInitialized) {
            stateMachine.shutdown()
        }
    }

    private fun isProtectionEnabled(): Boolean {
        return ProtectionCache.isProtectionEnabled(this)
    }
}