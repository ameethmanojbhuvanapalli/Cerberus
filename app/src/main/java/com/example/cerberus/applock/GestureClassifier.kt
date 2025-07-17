package com.example.cerberus.applock

import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Classifies user gestures and accessibility events to determine
 * whether the user is performing navigation vs background actions.
 * This helps the EventProcessor make intelligent decisions about
 * app state transitions.
 */
class GestureClassifier {
    private val TAG = "GestureClassifier"
    
    // System packages that indicate navigation or system UI
    private val systemNavigationPackages = setOf(
        "com.android.systemui",
        "android",
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher3",
        "com.miui.home",
        "com.huawei.android.launcher",
        "com.samsung.android.app.launcher",
        "com.oneplus.launcher"
    )
    
    // Activity class names that indicate background gestures
    private val backgroundActivityClasses = setOf(
        "com.android.systemui.recents.RecentsActivity",
        "com.android.launcher3.Launcher",
        "com.miui.home.launcher.Launcher"
    )
    
    data class GestureClassification(
        val isBackgroundGesture: Boolean,
        val isSystemNavigation: Boolean,
        val isAppSwitch: Boolean,
        val confidence: Float // 0.0 to 1.0
    )
    
    /**
     * Classify an accessibility event to determine the type of user interaction
     */
    fun classifyEvent(
        event: AccessibilityEvent?,
        previousPackage: String?,
        currentPackage: String?
    ): GestureClassification {
        if (event == null || currentPackage == null) {
            return GestureClassification(
                isBackgroundGesture = false,
                isSystemNavigation = false,
                isAppSwitch = false,
                confidence = 0.0f
            )
        }
        
        val packageName = event.packageName?.toString()
        val className = event.className?.toString()
        
        Log.d(TAG, "Classifying event: package=$packageName, class=$className, " +
                "prev=$previousPackage, current=$currentPackage")
        
        // High confidence background gesture indicators
        if (isSystemNavigationPackage(packageName)) {
            Log.d(TAG, "Detected system navigation package: $packageName")
            return GestureClassification(
                isBackgroundGesture = true,
                isSystemNavigation = true,
                isAppSwitch = previousPackage != null && previousPackage != currentPackage,
                confidence = 0.9f
            )
        }
        
        // Check for specific background activity classes
        if (isBackgroundActivityClass(className)) {
            Log.d(TAG, "Detected background activity class: $className")
            return GestureClassification(
                isBackgroundGesture = true,
                isSystemNavigation = true,
                isAppSwitch = true,
                confidence = 0.85f
            )
        }
        
        // App switch detection
        val isAppSwitch = isAppSwitchDetected(previousPackage, currentPackage)
        if (isAppSwitch) {
            Log.d(TAG, "App switch detected: $previousPackage -> $currentPackage")
            return GestureClassification(
                isBackgroundGesture = true,
                isSystemNavigation = false,
                isAppSwitch = true,
                confidence = 0.7f
            )
        }
        
        // Default: not a background gesture
        return GestureClassification(
            isBackgroundGesture = false,
            isSystemNavigation = false,
            isAppSwitch = false,
            confidence = 0.6f
        )
    }
    
    /**
     * Determine if this represents a return to a previously backgrounded app
     */
    fun isAppReturn(
        previousPackage: String?,
        currentPackage: String?,
        timeElapsed: Long
    ): Boolean {
        if (previousPackage == null || currentPackage == null) return false
        
        // If user is returning to the same app after a brief period, it's likely an app return
        val isReturnToSameApp = previousPackage == currentPackage
        val isRecentReturn = timeElapsed < 30000 // 30 seconds
        
        val isReturn = isReturnToSameApp && isRecentReturn
        if (isReturn) {
            Log.d(TAG, "App return detected: returning to $currentPackage after ${timeElapsed}ms")
        }
        
        return isReturn
    }
    
    /**
     * Check if the event indicates a user intentionally closing/exiting an app
     */
    fun isAppExitGesture(
        event: AccessibilityEvent?,
        previousPackage: String?,
        currentPackage: String?
    ): Boolean {
        if (event == null) return false
        
        val packageName = event.packageName?.toString()
        val className = event.className?.toString()
        
        // Detect back-to-home gestures
        val isHomeTransition = isSystemNavigationPackage(packageName) && 
                               previousPackage != null && 
                               !isSystemNavigationPackage(previousPackage)
        
        // Detect explicit app closing
        val isExplicitClose = className?.contains("launcher", ignoreCase = true) == true ||
                             className?.contains("home", ignoreCase = true) == true
        
        val isExit = isHomeTransition || isExplicitClose
        if (isExit) {
            Log.d(TAG, "App exit gesture detected: $previousPackage -> $packageName")
        }
        
        return isExit
    }
    
    private fun isSystemNavigationPackage(packageName: String?): Boolean {
        return packageName in systemNavigationPackages
    }
    
    private fun isBackgroundActivityClass(className: String?): Boolean {
        if (className == null) return false
        return backgroundActivityClasses.any { className.contains(it) }
    }
    
    private fun isAppSwitchDetected(previousPackage: String?, currentPackage: String?): Boolean {
        if (previousPackage == null || currentPackage == null) return false
        
        // Different packages = app switch
        val isDifferentPackage = previousPackage != currentPackage
        
        // But exclude transitions to/from system packages
        val fromSystem = isSystemNavigationPackage(previousPackage)
        val toSystem = isSystemNavigationPackage(currentPackage)
        
        return isDifferentPackage && !fromSystem && !toSystem
    }
}