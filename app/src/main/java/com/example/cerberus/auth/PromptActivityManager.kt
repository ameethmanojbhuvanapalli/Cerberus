package com.example.cerberus.auth

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton manager to coordinate prompt activities and prevent race conditions.
 * Ensures only one authentication prompt can be active at a time per package.
 */
object PromptActivityManager {
    private val TAG = "PromptActivityManager"
    private val activePrompts = ConcurrentHashMap<String, String>() // packageName -> promptType
    private val promptLocks = ConcurrentHashMap<String, Any>() // packageName -> lock object
    
    /**
     * Attempts to register a prompt activity for the given package.
     * @param packageName The package requesting authentication
     * @param promptType The type of prompt (e.g., "biometric", "pin", "pattern", "password")
     * @return true if the prompt was successfully registered, false if another prompt is active
     */
    @Synchronized
    fun registerPrompt(packageName: String, promptType: String): Boolean {
        val existingPrompt = activePrompts[packageName]
        if (existingPrompt != null) {
            Log.d(TAG, "Prompt already active for $packageName: $existingPrompt. Rejecting $promptType")
            return false
        }
        
        activePrompts[packageName] = promptType
        promptLocks[packageName] = Any()
        Log.d(TAG, "Registered $promptType prompt for $packageName")
        return true
    }
    
    /**
     * Unregisters the prompt activity for the given package.
     * @param packageName The package to unregister
     * @param promptType The type of prompt being unregistered
     */
    @Synchronized
    fun unregisterPrompt(packageName: String, promptType: String) {
        val currentPrompt = activePrompts[packageName]
        if (currentPrompt == promptType) {
            activePrompts.remove(packageName)
            promptLocks.remove(packageName)
            Log.d(TAG, "Unregistered $promptType prompt for $packageName")
        } else {
            Log.w(TAG, "Attempted to unregister $promptType for $packageName, but current prompt is $currentPrompt")
        }
    }
    
    /**
     * Checks if a prompt is currently active for the given package.
     * @param packageName The package to check
     * @return true if a prompt is active, false otherwise
     */
    @Synchronized
    fun isPromptActive(packageName: String): Boolean {
        return activePrompts.containsKey(packageName)
    }
    
    /**
     * Gets the lock object for synchronization on a specific package.
     * @param packageName The package to get the lock for
     * @return The lock object, or null if no prompt is registered
     */
    @Synchronized
    fun getPromptLock(packageName: String): Any? {
        return promptLocks[packageName]
    }
    
    /**
     * Gets the active prompt type for a package.
     * @param packageName The package to check
     * @return The prompt type, or null if no prompt is active
     */
    @Synchronized
    fun getActivePromptType(packageName: String): String? {
        return activePrompts[packageName]
    }
    
    /**
     * Clears all active prompts. Used for cleanup scenarios.
     */
    @Synchronized
    fun clearAllPrompts() {
        Log.d(TAG, "Clearing all active prompts")
        activePrompts.clear()
        promptLocks.clear()
    }
}