package com.example.cerberus.applock

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.auth.AuthenticationManager
import com.example.cerberus.auth.AuthChannel
import com.example.cerberus.data.LockedAppsCache
import com.example.cerberus.statemachine.AppEvent
import com.example.cerberus.statemachine.AppState
import com.example.cerberus.statemachine.AppStateMachine
import com.example.cerberus.statemachine.StateTransitionListener
import java.util.concurrent.ConcurrentHashMap

/**
 * Processes accessibility events and maps them to app events for the state machine.
 * This class serves as the bridge between the AccessibilityService and the
 * DFA-based state machine, implementing intelligent event processing and
 * gesture classification.
 */
class EventProcessor(
    private val context: Context,
    private val stateMachine: AppStateMachine,
    private val gestureClassifier: GestureClassifier = GestureClassifier()
) : StateTransitionListener {
    
    private val TAG = "EventProcessor"
    private val handler = Handler(Looper.getMainLooper())
    
    // Event processing state
    private var lastPackageName: String? = null
    private var lastClassName: String? = null
    private var lastEventTime: Long = 0L
    
    // Debouncing for stable detection
    private var stablePromptRunnable: Runnable? = null
    private var activityChangeCount: Int = 0
    private val STABLE_DELAY = 500L
    
    // App exit debouncing
    private var appExitRunnable: Runnable? = null
    private val APP_EXIT_DELAY = 1500L
    private var pendingAppExitPackage: String? = null
    
    // Track app launch times for return detection
    private val appLaunchTimes = ConcurrentHashMap<String, Long>()
    
    private val authService
        get() = AuthenticationManager.getInstance(context).getAuthService()
    
    init {
        stateMachine.addStateTransitionListener(this)
        Log.d(TAG, "EventProcessor initialized")
    }
    
    /**
     * Process an accessibility event and determine appropriate state machine events
     */
    fun processAccessibilityEvent(event: AccessibilityEvent?, myPackageName: String, promptActivityName: String) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        
        val foregroundPackage = event.packageName?.toString() ?: return
        val foregroundClass = event.className?.toString() ?: return
        val currentTime = System.currentTimeMillis()
        
        Log.d(TAG, "Processing event: package=$foregroundPackage, class=$foregroundClass")
        
        // Skip system packages
        if (isSystemPackage(foregroundPackage)) {
            updateLastEvent(foregroundPackage, foregroundClass, currentTime)
            return
        }
        
        // Don't process if prompt activity is being shown
        if (foregroundPackage == myPackageName && foregroundClass.contains(promptActivityName)) {
            updateLastEvent(foregroundPackage, foregroundClass, currentTime)
            return
        }
        
        val lockedApps = LockedAppsCache.getLockedApps(context).toMutableSet().apply { add(myPackageName) }
        
        // Classify the gesture/event
        val gestureClassification = gestureClassifier.classifyEvent(
            event, lastPackageName, foregroundPackage
        )
        
        // Process app exit detection
        processAppExitDetection(foregroundPackage, lockedApps, currentTime)
        
        // Process app launch/return detection
        processAppLaunchOrReturn(foregroundPackage, lockedApps, gestureClassification, currentTime)
        
        updateLastEvent(foregroundPackage, foregroundClass, currentTime)
    }
    
    private fun processAppExitDetection(foregroundPackage: String, lockedApps: Set<String>, currentTime: Long) {
        if (lastPackageName != null && 
            lastPackageName != foregroundPackage && 
            lockedApps.contains(lastPackageName)) {
            
            // User appears to have left a locked app
            pendingAppExitPackage = lastPackageName
            
            // Cancel any previous pending exit
            appExitRunnable?.let { handler.removeCallbacks(it) }
            
            appExitRunnable = Runnable {
                val exitPackage = pendingAppExitPackage
                if (exitPackage != null && exitPackage != foregroundPackage) {
                    Log.d(TAG, "Processing app exit for: $exitPackage")
                    
                    val currentState = stateMachine.getCurrentState(exitPackage)
                    when (currentState) {
                        AppState.AUTHENTICATED, AppState.FOREGROUND -> {
                            stateMachine.processEvent(exitPackage, AppEvent.BACKGROUND_GESTURE)
                        }
                        AppState.AUTHENTICATING -> {
                            stateMachine.processEvent(exitPackage, AppEvent.BACKGROUND_GESTURE)
                        }
                        else -> {
                            Log.d(TAG, "App $exitPackage already in appropriate state: $currentState")
                        }
                    }
                    
                    // Update authentication expiration
                    authService.updateExpirationForAppExit(exitPackage)
                } else {
                    Log.d(TAG, "Debounced: User returned to locked app, not processing exit")
                }
                pendingAppExitPackage = null
            }
            
            handler.postDelayed(appExitRunnable!!, APP_EXIT_DELAY)
        }
    }
    
    private fun processAppLaunchOrReturn(
        foregroundPackage: String,
        lockedApps: Set<String>,
        gestureClassification: GestureClassifier.GestureClassification,
        currentTime: Long
    ) {
        if (!lockedApps.contains(foregroundPackage)) return
        
        val currentState = stateMachine.getCurrentState(foregroundPackage)
        val lastLaunchTime = appLaunchTimes[foregroundPackage]
        val timeElapsed = if (lastLaunchTime != null) currentTime - lastLaunchTime else Long.MAX_VALUE
        
        // Determine if this is an app return vs new launch
        val isAppReturn = currentState == AppState.BACKGROUND && 
                         gestureClassifier.isAppReturn(lastPackageName, foregroundPackage, timeElapsed)
        
        if (isAppReturn) {
            Log.d(TAG, "App return detected for: $foregroundPackage")
            stateMachine.processEvent(foregroundPackage, AppEvent.APP_RETURNED)
        } else if (currentState == AppState.IDLE || currentState == AppState.EXITED) {
            Log.d(TAG, "App launch detected for: $foregroundPackage")
            stateMachine.processEvent(foregroundPackage, AppEvent.APP_LAUNCHED)
            appLaunchTimes[foregroundPackage] = currentTime
        }
        
        // Check if authentication is needed
        checkAuthenticationNeeded(foregroundPackage, currentState, currentTime)
    }
    
    private fun checkAuthenticationNeeded(packageName: String, currentState: AppState, currentTime: Long) {
        // Only trigger authentication for foreground apps that aren't already authenticated
        if (currentState == AppState.FOREGROUND && 
            lastPackageName != packageName && 
            !authService.isAuthenticated(packageName)) {
            
            activityChangeCount = 1
            
            // Cancel any previous stable prompt
            stablePromptRunnable?.let { handler.removeCallbacks(it) }
            
            stablePromptRunnable = Runnable {
                // Only prompt if still in foreground state
                if (stateMachine.getCurrentState(packageName) == AppState.FOREGROUND) {
                    Log.d(TAG, "Triggering authentication prompt for: $packageName")
                    
                    // Transition to authenticating state
                    if (stateMachine.processEvent(packageName, AppEvent.AUTH_PROMPT_SHOWN)) {
                        requestAuthentication(packageName)
                    }
                }
            }
            
            handler.postDelayed(stablePromptRunnable!!, STABLE_DELAY)
        }
    }
    
    private fun requestAuthentication(packageName: String) {
        authService.requestAuthenticationIfNeeded(
            AuthChannel.APPLOCK,
            packageName,
            object : AuthenticationCallback {
                override fun onAuthenticationSucceeded(packageName: String) {
                    Log.d(TAG, "Authentication succeeded for: $packageName")
                    stateMachine.processEvent(packageName, AppEvent.AUTH_SUCCESS)
                }
                
                override fun onAuthenticationFailed(packageName: String) {
                    Log.d(TAG, "Authentication failed for: $packageName")
                    stateMachine.processEvent(packageName, AppEvent.AUTH_FAILED)
                }
            }
        )
    }
    
    private fun updateLastEvent(packageName: String, className: String, time: Long) {
        lastPackageName = packageName
        lastClassName = className
        lastEventTime = time
    }
    
    private fun isSystemPackage(packageName: String): Boolean {
        return packageName in setOf("com.android.systemui", "android") || packageName.isBlank()
    }
    
    // StateTransitionListener implementation
    override fun onStateChanged(
        packageName: String,
        fromState: AppState,
        toState: AppState,
        event: AppEvent
    ) {
        Log.d(TAG, "State change notification: $packageName $fromState -> $toState (event: $event)")
        
        // Handle timeout management based on state changes
        when (toState) {
            AppState.BACKGROUND -> {
                // App moved to background, start timeout if authenticated
                if (fromState == AppState.AUTHENTICATED) {
                    Log.d(TAG, "Starting timeout management for backgrounded app: $packageName")
                }
            }
            AppState.EXITED -> {
                // Clean up any timers for this app
                appLaunchTimes.remove(packageName)
                Log.d(TAG, "Cleaned up tracking for exited app: $packageName")
            }
            else -> {
                // No special handling needed
            }
        }
    }
    
    override fun onTransitionRejected(
        packageName: String,
        currentState: AppState,
        event: AppEvent
    ) {
        Log.w(TAG, "Transition rejected for $packageName: $currentState + $event")
    }
    
    /**
     * Clean up resources
     */
    fun shutdown() {
        stateMachine.removeStateTransitionListener(this)
        stablePromptRunnable?.let { handler.removeCallbacks(it) }
        appExitRunnable?.let { handler.removeCallbacks(it) }
        appLaunchTimes.clear()
        Log.d(TAG, "EventProcessor shutdown completed")
    }
}