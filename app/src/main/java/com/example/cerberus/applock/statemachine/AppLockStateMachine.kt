package com.example.cerberus.applock.statemachine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.auth.AuthenticationManager
import com.example.cerberus.auth.AuthChannel

/**
 * Core state machine for the app lock service that manages state transitions
 * and ensures consistent, predictable behavior across all app interactions.
 */
class AppLockStateMachine(
    private val context: Context
) {
    
    private val TAG = "AppLockStateMachine"
    private val handler = Handler(Looper.getMainLooper())
    
    // Current state
    private var currentState: LockState = LockState.IDLE
    private var currentPackageName: String? = null
    private var currentClassName: String? = null
    
    // Settlement period for stable app detection
    private var settlementRunnable: Runnable? = null
    private val STABLE_DELAY = 500L // ms
    
    // App exit timeout handling
    private var appExitRunnable: Runnable? = null
    private val APP_EXIT_DELAY = 1500L // ms
    
    // Authentication service
    private val authService
        get() = AuthenticationManager.getInstance(context).getAuthService()
    
    /**
     * Processes a lock event and potentially transitions to a new state.
     * 
     * @param event The event to process
     * @return true if the event was processed and potentially caused a state change
     */
    fun processEvent(event: LockEvent): Boolean {
        Log.d(TAG, "Processing event: $event in state: $currentState")
        
        // Handle app exit detection for authenticated apps
        if (currentState == LockState.AUTHENTICATED && 
            (event is LockEvent.LockedAppOpened || event is LockEvent.NonLockedAppOpened || event is LockEvent.CerberusAppOpened)) {
            
            // Check if we're leaving the authenticated app
            val newPackageName = when (event) {
                is LockEvent.LockedAppOpened -> event.packageName
                is LockEvent.NonLockedAppOpened -> event.packageName
                is LockEvent.CerberusAppOpened -> null
                else -> null
            }
            
            if (currentPackageName != null && currentPackageName != newPackageName) {
                // Process app exit first
                val appExitEvent = LockEvent.AppLeft(currentPackageName!!)
                processAppExitEvent(appExitEvent)
            }
        }
        
        // Check if event should be ignored in current state
        if (StateValidator.shouldIgnoreEvent(currentState, event)) {
            Log.d(TAG, "Ignoring event $event in state $currentState")
            return false
        }
        
        // Determine the target state for this event
        val targetState = determineTargetState(event)
        if (targetState == null) {
            Log.d(TAG, "No state change needed for event: $event")
            return false
        }
        
        // Validate the transition
        if (!StateValidator.isValidTransition(currentState, event, targetState)) {
            val reason = StateValidator.getTransitionErrorReason(currentState, event, targetState)
            Log.w(TAG, "Invalid transition blocked: $reason")
            return false
        }
        
        // Perform the state transition
        performTransition(event, targetState)
        return true
    }
    
    /**
     * Processes app exit events separately to handle timeout logic
     */
    private fun processAppExitEvent(event: LockEvent.AppLeft) {
        Log.d(TAG, "Processing app exit: ${event.packageName}")
        
        // Schedule the app exit timeout
        scheduleAppExitTimeout(event.packageName)
        
        // Transition to idle state
        performTransition(event, LockState.IDLE)
    }
    
    /**
     * Determines the target state for a given event in the current state
     */
    private fun determineTargetState(event: LockEvent): LockState? {
        return when (currentState) {
            LockState.IDLE -> determineTargetFromIdle(event)
            LockState.LOCKED_APP_DETECTED -> determineTargetFromLockedAppDetected(event)
            LockState.PROMPTING -> determineTargetFromPrompting(event)
            LockState.AUTHENTICATED -> determineTargetFromAuthenticated(event)
        }
    }
    
    private fun determineTargetFromIdle(event: LockEvent): LockState? {
        return when (event) {
            is LockEvent.LockedAppOpened -> LockState.LOCKED_APP_DETECTED
            is LockEvent.NonLockedAppOpened -> LockState.IDLE
            is LockEvent.CerberusAppOpened -> LockState.IDLE
            is LockEvent.SystemPackageDetected -> LockState.IDLE
            is LockEvent.SameAppActivityChanged -> LockState.IDLE
            is LockEvent.Reset -> LockState.IDLE
            else -> null
        }
    }
    
    private fun determineTargetFromLockedAppDetected(event: LockEvent): LockState? {
        return when (event) {
            is LockEvent.SettlementCompleted -> {
                // Only transition to prompting if still in the same app and not authenticated
                if (currentPackageName != null && !authService.isAuthenticated(currentPackageName!!)) {
                    LockState.PROMPTING
                } else {
                    LockState.IDLE
                }
            }
            is LockEvent.NonLockedAppOpened -> LockState.IDLE
            is LockEvent.LockedAppOpened -> LockState.LOCKED_APP_DETECTED
            is LockEvent.CerberusAppOpened -> LockState.IDLE
            is LockEvent.SystemPackageDetected -> null // Stay in same state
            is LockEvent.SameAppActivityChanged -> null // Stay in same state
            is LockEvent.Reset -> LockState.IDLE
            else -> null
        }
    }
    
    private fun determineTargetFromPrompting(event: LockEvent): LockState? {
        return when (event) {
            is LockEvent.AuthenticationSucceeded -> LockState.AUTHENTICATED
            is LockEvent.AuthenticationFailed -> LockState.IDLE
            is LockEvent.NonLockedAppOpened -> LockState.IDLE
            is LockEvent.CerberusAppOpened -> LockState.IDLE
            is LockEvent.Reset -> LockState.IDLE
            else -> null // Ignore other events while prompting
        }
    }
    
    private fun determineTargetFromAuthenticated(event: LockEvent): LockState? {
        return when (event) {
            is LockEvent.AppLeft -> LockState.IDLE
            is LockEvent.NonLockedAppOpened -> LockState.IDLE
            is LockEvent.LockedAppOpened -> LockState.LOCKED_APP_DETECTED
            is LockEvent.CerberusAppOpened -> LockState.IDLE
            is LockEvent.SameAppActivityChanged -> LockState.AUTHENTICATED
            is LockEvent.SystemPackageDetected -> null // Stay in same state
            is LockEvent.Reset -> LockState.IDLE
            else -> null
        }
    }
    
    /**
     * Performs the actual state transition and associated side effects
     */
    private fun performTransition(event: LockEvent, targetState: LockState) {
        val previousState = currentState
        
        // Cancel any pending operations when changing states
        cancelPendingOperations()
        
        // Update state
        currentState = targetState
        
        // Update tracking variables based on event
        updateTrackingVariables(event)
        
        // Perform state-specific actions
        performStateActions(event, targetState)
        
        Log.i(TAG, "State transition: $previousState -> $targetState via $event")
    }
    
    /**
     * Updates tracking variables based on the event
     */
    private fun updateTrackingVariables(event: LockEvent) {
        when (event) {
            is LockEvent.LockedAppOpened -> {
                currentPackageName = event.packageName
                currentClassName = event.className
            }
            is LockEvent.NonLockedAppOpened -> {
                currentPackageName = event.packageName
                currentClassName = null
            }
            is LockEvent.SameAppActivityChanged -> {
                currentClassName = event.className
            }
            is LockEvent.AppLeft,
            is LockEvent.CerberusAppOpened,
            is LockEvent.Reset -> {
                currentPackageName = null
                currentClassName = null
            }
            else -> {
                // No changes needed for other events
            }
        }
    }
    
    /**
     * Performs actions specific to the new state
     */
    private fun performStateActions(event: LockEvent, targetState: LockState) {
        when (targetState) {
            LockState.IDLE -> {
                // No additional actions needed when transitioning to idle
                // App exit timeout is handled separately
            }
            
            LockState.LOCKED_APP_DETECTED -> {
                // Start settlement period
                if (event is LockEvent.LockedAppOpened) {
                    scheduleSettlementTimeout()
                }
            }
            
            LockState.PROMPTING -> {
                // Trigger authentication prompt
                if (currentPackageName != null) {
                    triggerAuthentication(currentPackageName!!)
                }
            }
            
            LockState.AUTHENTICATED -> {
                // Authentication successful - no additional actions needed
                Log.d(TAG, "App authenticated: $currentPackageName")
            }
        }
    }
    
    /**
     * Schedules the settlement timeout for locked app detection
     */
    private fun scheduleSettlementTimeout() {
        settlementRunnable = Runnable {
            processEvent(LockEvent.SettlementCompleted)
        }
        handler.postDelayed(settlementRunnable!!, STABLE_DELAY)
        Log.d(TAG, "Settlement timeout scheduled for ${STABLE_DELAY}ms")
    }
    
    /**
     * Schedules app exit timeout to update authentication expiration
     */
    private fun scheduleAppExitTimeout(packageName: String) {
        appExitRunnable = Runnable {
            authService.updateExpirationForAppExit(packageName)
            Log.d(TAG, "App exit timeout completed for: $packageName")
        }
        handler.postDelayed(appExitRunnable!!, APP_EXIT_DELAY)
        Log.d(TAG, "App exit timeout scheduled for: $packageName")
    }
    
    /**
     * Triggers authentication for the specified package
     */
    private fun triggerAuthentication(packageName: String) {
        Log.d(TAG, "Triggering authentication for: $packageName")
        
        val callback = object : AuthenticationCallback {
            override fun onAuthenticationSucceeded(packageName: String) {
                Log.d(TAG, "Authentication succeeded for: $packageName")
                processEvent(LockEvent.AuthenticationSucceeded(packageName))
            }
            
            override fun onAuthenticationFailed(packageName: String) {
                Log.d(TAG, "Authentication failed for: $packageName")
                processEvent(LockEvent.AuthenticationFailed(packageName))
            }
        }
        
        authService.requestAuthenticationIfNeeded(
            AuthChannel.APPLOCK,
            packageName,
            callback
        )
    }
    
    /**
     * Cancels any pending timeout operations
     */
    private fun cancelPendingOperations() {
        settlementRunnable?.let {
            handler.removeCallbacks(it)
            settlementRunnable = null
        }
        
        appExitRunnable?.let {
            handler.removeCallbacks(it)
            appExitRunnable = null
        }
    }
    
    /**
     * Gets the current state of the state machine
     */
    fun getCurrentState(): LockState = currentState
    
    /**
     * Gets the current package name being tracked
     */
    fun getCurrentPackageName(): String? = currentPackageName
    
    /**
     * Resets the state machine to idle state
     */
    fun reset() {
        Log.i(TAG, "Resetting state machine")
        processEvent(LockEvent.Reset)
    }
    
    /**
     * Shuts down the state machine and cleans up resources
     */
    fun shutdown() {
        Log.i(TAG, "Shutting down state machine")
        cancelPendingOperations()
        currentState = LockState.IDLE
        currentPackageName = null
        currentClassName = null
    }
}