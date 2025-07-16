package com.example.cerberus.lifecycle

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.cerberus.auth.listeners.AppLifecycleListener

/**
 * Clean app lifecycle detector with debouncing and filtering.
 * Handles true app exit detection, prompt activity filtering, and system package filtering.
 */
class AppLifecycleDetector(
    private val cerberusPackageName: String
) {
    private val listeners = mutableListOf<AppLifecycleListener>()
    private val handler = Handler(Looper.getMainLooper())
    
    // Debouncing for app exit detection
    private var appExitRunnable: Runnable? = null
    private var pendingAppExitPackage: String? = null
    private val APP_EXIT_DELAY = 1500L // ms
    
    // System packages to ignore
    private val systemPackages = setOf("com.android.systemui", "android", null)
    
    // Cerberus authentication prompt activities
    private val cerberusPromptActivities = setOf(
        "BiometricPromptActivity",
        "PinPromptActivity", 
        "PasswordPromptActivity",
        "PatternPromptActivity"
    )
    
    private var lastPackageName: String? = null
    
    companion object {
        private const val TAG = "AppLifecycleDetector"
    }

    /**
     * Process window state change events
     */
    fun onWindowStateChanged(
        packageName: String,
        className: String,
        lockedApps: Set<String>
    ) {
        // Filter system packages
        if (systemPackages.contains(packageName)) {
            return
        }

        // Handle Cerberus prompt activities
        if (packageName == cerberusPackageName && isCerberusPromptActivity(className)) {
            Log.d(TAG, "Detected Cerberus prompt activity: $className")
            notifyPromptActivityDetected(packageName, className)
            lastPackageName = packageName
            return
        }

        // Handle app exit detection with debouncing
        handleAppExitDetection(packageName, lockedApps)

        // Handle app entry
        if (lastPackageName != packageName) {
            Log.d(TAG, "App entered: $packageName")
            notifyAppEntered(packageName)
        }

        lastPackageName = packageName
    }

    /**
     * Handle app exit detection with debouncing
     */
    private fun handleAppExitDetection(
        currentPackage: String,
        lockedApps: Set<String>
    ) {
        if (lastPackageName != null && 
            lastPackageName != currentPackage && 
            lockedApps.contains(lastPackageName)) {
            
            // User appears to have left a locked app
            pendingAppExitPackage = lastPackageName
            
            // Cancel any previous pending exit
            appExitRunnable?.let { handler.removeCallbacks(it) }
            
            appExitRunnable = Runnable {
                // Only notify exit if user did NOT return to the locked app
                if (pendingAppExitPackage != currentPackage) {
                    Log.d(TAG, "App exited: $pendingAppExitPackage")
                    notifyAppExited(pendingAppExitPackage!!)
                } else {
                    Log.d(TAG, "Debounced: User returned to locked app $pendingAppExitPackage")
                }
                pendingAppExitPackage = null
            }
            
            handler.postDelayed(appExitRunnable!!, APP_EXIT_DELAY)
        }
    }

    /**
     * Check if a class name corresponds to a Cerberus prompt activity
     */
    private fun isCerberusPromptActivity(className: String): Boolean {
        return cerberusPromptActivities.any { className.contains(it) }
    }

    /**
     * Register a lifecycle listener
     */
    fun registerListener(listener: AppLifecycleListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
            Log.d(TAG, "Registered lifecycle listener: ${listener::class.java.simpleName}")
        }
    }

    /**
     * Unregister a lifecycle listener
     */
    fun unregisterListener(listener: AppLifecycleListener) {
        listeners.remove(listener)
        Log.d(TAG, "Unregistered lifecycle listener: ${listener::class.java.simpleName}")
    }

    /**
     * Get the last detected package
     */
    fun getLastPackage(): String? = lastPackageName

    /**
     * Check if a package is a system package
     */
    fun isSystemPackage(packageName: String): Boolean {
        return systemPackages.contains(packageName)
    }

    /**
     * Check if a package/class is a Cerberus prompt
     */
    fun isCerberusPrompt(packageName: String, className: String): Boolean {
        return packageName == cerberusPackageName && isCerberusPromptActivity(className)
    }

    private fun notifyAppEntered(packageName: String) {
        listeners.forEach { listener ->
            try {
                listener.onAppEntered(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying app entered", e)
            }
        }
    }

    private fun notifyAppExited(packageName: String) {
        listeners.forEach { listener ->
            try {
                listener.onAppExited(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying app exited", e)
            }
        }
    }

    private fun notifyPromptActivityDetected(packageName: String, className: String) {
        listeners.forEach { listener ->
            try {
                listener.onPromptActivityDetected(packageName, className)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying prompt activity detected", e)
            }
        }
    }

    /**
     * Cleanup resources
     */
    fun shutdown() {
        Log.d(TAG, "Shutting down app lifecycle detector")
        appExitRunnable?.let { handler.removeCallbacks(it) }
        listeners.clear()
        pendingAppExitPackage = null
        lastPackageName = null
    }
}