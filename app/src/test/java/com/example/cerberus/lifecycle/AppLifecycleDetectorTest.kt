package com.example.cerberus.lifecycle

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import com.example.cerberus.auth.listeners.AppLifecycleListener

/**
 * Unit tests for AppLifecycleDetector.
 * Tests the critical authentication loop fix.
 */
class AppLifecycleDetectorTest {
    
    private lateinit var detector: AppLifecycleDetector
    private val cerberusPackage = "com.example.cerberus"
    private val lockedApps = setOf("com.example.app1", "com.example.app2")
    
    @Before
    fun setUp() {
        detector = AppLifecycleDetector(cerberusPackage)
    }

    @Test
    fun testSystemPackageFiltering() {
        // System packages should be ignored
        assertTrue(detector.isSystemPackage("com.android.systemui"))
        assertTrue(detector.isSystemPackage("android"))
        assertTrue(detector.isSystemPackage(null))
        assertFalse(detector.isSystemPackage("com.example.app"))
    }

    @Test
    fun testCerberusPromptActivityDetection() {
        val biometricClass = "com.example.cerberus.ui.activity.BiometricPromptActivity"
        val pinClass = "com.example.cerberus.ui.activity.PinPromptActivity"
        val passwordClass = "com.example.cerberus.ui.activity.PasswordPromptActivity"
        val patternClass = "com.example.cerberus.ui.activity.PatternPromptActivity"
        val normalClass = "com.example.cerberus.MainActivity"
        
        // All prompt activities should be detected
        assertTrue(detector.isCerberusPrompt(cerberusPackage, biometricClass))
        assertTrue(detector.isCerberusPrompt(cerberusPackage, pinClass))
        assertTrue(detector.isCerberusPrompt(cerberusPackage, passwordClass))
        assertTrue(detector.isCerberusPrompt(cerberusPackage, patternClass))
        
        // Normal activities should not be detected as prompts
        assertFalse(detector.isCerberusPrompt(cerberusPackage, normalClass))
        
        // Other apps' activities should not be detected as Cerberus prompts
        assertFalse(detector.isCerberusPrompt("com.other.app", biometricClass))
    }

    @Test
    fun testCerberusPromptActivitiesAreIgnored() {
        var promptDetectedCount = 0
        var appEnteredCount = 0
        var appExitedCount = 0
        
        val listener = object : AppLifecycleListener {
            override fun onAppEntered(packageName: String) {
                appEnteredCount++
            }
            
            override fun onAppExited(packageName: String) {
                appExitedCount++
            }
            
            override fun onPromptActivityDetected(packageName: String, className: String) {
                promptDetectedCount++
            }
        }
        
        detector.registerListener(listener)
        
        // Simulate Cerberus prompt activity appearing
        detector.onWindowStateChanged(
            cerberusPackage,
            "com.example.cerberus.ui.activity.BiometricPromptActivity",
            lockedApps
        )
        
        // Should detect prompt but not trigger app entered
        assertEquals(1, promptDetectedCount)
        assertEquals(0, appEnteredCount) // Prompt activities should not count as app entry
        assertEquals(0, appExitedCount)
    }

    @Test
    fun testAppEntryDetection() {
        var appEnteredPackage: String? = null
        
        val listener = object : AppLifecycleListener {
            override fun onAppEntered(packageName: String) {
                appEnteredPackage = packageName
            }
            
            override fun onAppExited(packageName: String) {}
            override fun onPromptActivityDetected(packageName: String, className: String) {}
        }
        
        detector.registerListener(listener)
        
        // First app entry
        detector.onWindowStateChanged("com.example.app1", "MainActivity", lockedApps)
        assertEquals("com.example.app1", appEnteredPackage)
        
        // Second app entry (should replace first)
        detector.onWindowStateChanged("com.example.app2", "MainActivity", lockedApps)
        assertEquals("com.example.app2", appEnteredPackage)
    }

    @Test
    fun testAppExitWithDebouncing() {
        var exitedApps = mutableListOf<String>()
        
        val listener = object : AppLifecycleListener {
            override fun onAppEntered(packageName: String) {}
            
            override fun onAppExited(packageName: String) {
                exitedApps.add(packageName)
            }
            
            override fun onPromptActivityDetected(packageName: String, className: String) {}
        }
        
        detector.registerListener(listener)
        
        // Enter first app
        detector.onWindowStateChanged("com.example.app1", "MainActivity", lockedApps)
        
        // Enter second app (should trigger exit of first app after debounce)
        detector.onWindowStateChanged("com.example.app2", "MainActivity", lockedApps)
        
        // For unit test, we can't easily test the debounce timing, but we can verify the structure
        // In practice, the debounce would trigger after APP_EXIT_DELAY ms
        assertTrue(exitedApps.isEmpty()) // Should be empty immediately due to debouncing
    }

    @Test
    fun testNoSelfAuthenticationLoop() {
        var cerberusEntryCount = 0
        var cerberusExitCount = 0
        
        val listener = object : AppLifecycleListener {
            override fun onAppEntered(packageName: String) {
                if (packageName == cerberusPackage) {
                    cerberusEntryCount++
                }
            }
            
            override fun onAppExited(packageName: String) {
                if (packageName == cerberusPackage) {
                    cerberusExitCount++
                }
            }
            
            override fun onPromptActivityDetected(packageName: String, className: String) {}
        }
        
        detector.registerListener(listener)
        
        // Test sequence that would previously cause authentication loop:
        // 1. Regular app appears
        detector.onWindowStateChanged("com.example.app1", "MainActivity", lockedApps)
        
        // 2. Cerberus authentication prompt appears (this should NOT trigger Cerberus as "entered")
        detector.onWindowStateChanged(
            cerberusPackage,
            "com.example.cerberus.ui.activity.BiometricPromptActivity",
            lockedApps
        )
        
        // 3. Back to regular app
        detector.onWindowStateChanged("com.example.app1", "MainActivity", lockedApps)
        
        // Cerberus should never be counted as "entered" during prompt activities
        assertEquals(0, cerberusEntryCount)
        assertEquals(0, cerberusExitCount)
    }

    @Test
    fun testMultiplePromptActivitiesHandled() {
        val promptActivities = listOf(
            "BiometricPromptActivity",
            "PinPromptActivity",
            "PasswordPromptActivity", 
            "PatternPromptActivity"
        )
        
        var promptCount = 0
        val listener = object : AppLifecycleListener {
            override fun onAppEntered(packageName: String) {}
            override fun onAppExited(packageName: String) {}
            override fun onPromptActivityDetected(packageName: String, className: String) {
                promptCount++
            }
        }
        
        detector.registerListener(listener)
        
        // Test each prompt activity type
        promptActivities.forEach { activity ->
            detector.onWindowStateChanged(
                cerberusPackage,
                "com.example.cerberus.ui.activity.$activity",
                lockedApps
            )
        }
        
        assertEquals(promptActivities.size, promptCount)
    }

    @Test
    fun testListenerManagement() {
        val listener1 = object : AppLifecycleListener {
            override fun onAppEntered(packageName: String) {}
            override fun onAppExited(packageName: String) {}
            override fun onPromptActivityDetected(packageName: String, className: String) {}
        }
        
        val listener2 = object : AppLifecycleListener {
            override fun onAppEntered(packageName: String) {}
            override fun onAppExited(packageName: String) {}
            override fun onPromptActivityDetected(packageName: String, className: String) {}
        }
        
        // Register listeners
        detector.registerListener(listener1)
        detector.registerListener(listener2)
        
        // Registering same listener again should not cause issues
        detector.registerListener(listener1)
        
        // Unregister listener
        detector.unregisterListener(listener1)
        
        // Should not throw exception
        detector.unregisterListener(listener2)
    }
}