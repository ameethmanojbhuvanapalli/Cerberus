package com.example.cerberus.applock.statemachine

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.cerberus.data.LockedAppsCache

/**
 * Processes accessibility events and converts them into appropriate LockEvents
 * for the state machine. Handles filtering of system packages, gesture animations,
 * and validation of event data.
 */
class EventProcessor(
    private val context: Context,
    private val cerberusPackageName: String
) {
    
    private val TAG = "EventProcessor"
    
    /**
     * Name of the authentication prompt activity to ignore Cerberus events during auth
     */
    private val promptActivityName = "com.example.cerberus.utils.BiometricPromptActivity"
    
    /**
     * Processes an accessibility event and returns the appropriate LockEvent.
     * Returns null if the event should be ignored.
     * 
     * @param event The accessibility event to process
     * @param lastPackageName The package name from the previous event (for comparison)
     * @return LockEvent if the event should be processed, null if it should be ignored
     */
    fun processEvent(event: AccessibilityEvent?, lastPackageName: String?): LockEvent? {
        // Only process window state changed events
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return null
        }
        
        val packageName = event.packageName?.toString()
        val className = event.className?.toString()
        
        // Log the event for debugging
        Log.d(TAG, "Processing event: package=$packageName, class=$className, last=$lastPackageName")
        
        // Filter out invalid events
        if (packageName == null || className == null) {
            Log.d(TAG, "Ignoring event with null package or class name")
            return null
        }
        
        // Filter out system packages and gesture animations
        if (SystemPackageFilter.isSystemPackage(packageName)) {
            Log.d(TAG, "Ignoring system package: $packageName")
            return LockEvent.SystemPackageDetected(packageName)
        }
        
        if (SystemPackageFilter.isGestureAnimation(packageName)) {
            Log.d(TAG, "Ignoring gesture animation: $packageName")
            return LockEvent.SystemPackageDetected(packageName)
        }
        
        // Handle Cerberus app specially
        if (packageName == cerberusPackageName) {
            return handleCerberusApp(className)
        }
        
        // Handle same app activity changes
        if (packageName == lastPackageName) {
            Log.d(TAG, "Same app activity change: $packageName")
            return LockEvent.SameAppActivityChanged(packageName, className)
        }
        
        // Check if this is a locked app
        val lockedApps = getLockedApps()
        val isLockedApp = lockedApps.contains(packageName)
        
        // Note: App exit events will be handled by the state machine when the new app opens
        // The state machine will detect the app change and handle the exit appropriately
        
        // Determine the event type for the new app
        return if (isLockedApp) {
            Log.d(TAG, "Locked app opened: $packageName")
            LockEvent.LockedAppOpened(packageName, className)
        } else {
            Log.d(TAG, "Non-locked app opened: $packageName")  
            LockEvent.NonLockedAppOpened(packageName)
        }
    }
    
    /**
     * Handles Cerberus app events specially to prevent infinite authentication loops
     */
    private fun handleCerberusApp(className: String): LockEvent? {
        // Ignore authentication prompt activities to prevent infinite loops
        if (className.contains(promptActivityName)) {
            Log.d(TAG, "Ignoring Cerberus prompt activity: $className")
            return null
        }
        
        // Allow access to Cerberus app itself
        Log.d(TAG, "Cerberus app opened: $className")
        return LockEvent.CerberusAppOpened
    }
    
    /**
     * Gets the current set of locked apps
     */
    private fun getLockedApps(): Set<String> {
        return LockedAppsCache.getLockedApps(context).toMutableSet().apply { 
            // Always include Cerberus in locked apps for consistency
            add(cerberusPackageName) 
        }
    }
}