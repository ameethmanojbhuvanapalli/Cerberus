package com.example.cerberus.applock.statemachine

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for LockEvent sealed class and all event types
 */
class LockEventTest {

    @Test
    fun testLockedAppOpenedEvent() {
        val event = LockEvent.LockedAppOpened("com.example.app", "MainActivity")
        assertEquals("com.example.app", event.packageName)
        assertEquals("MainActivity", event.className)
        assertTrue("Should be instance of LockEvent", event is LockEvent)
    }

    @Test
    fun testNonLockedAppOpenedEvent() {
        val event = LockEvent.NonLockedAppOpened("com.example.app")
        assertEquals("com.example.app", event.packageName)
        assertTrue("Should be instance of LockEvent", event is LockEvent)
    }

    @Test
    fun testSameAppActivityChangedEvent() {
        val event = LockEvent.SameAppActivityChanged("com.example.app", "SettingsActivity")
        assertEquals("com.example.app", event.packageName)
        assertEquals("SettingsActivity", event.className)
        assertTrue("Should be instance of LockEvent", event is LockEvent)
    }

    @Test
    fun testAuthenticationSucceededEvent() {
        val event = LockEvent.AuthenticationSucceeded("com.example.app")
        assertEquals("com.example.app", event.packageName)
        assertTrue("Should be instance of LockEvent", event is LockEvent)
    }

    @Test
    fun testAuthenticationFailedEvent() {
        val event = LockEvent.AuthenticationFailed("com.example.app")
        assertEquals("com.example.app", event.packageName)
        assertTrue("Should be instance of LockEvent", event is LockEvent)
    }

    @Test
    fun testAppLeftEvent() {
        val event = LockEvent.AppLeft("com.example.app")
        assertEquals("com.example.app", event.packageName)
        assertTrue("Should be instance of LockEvent", event is LockEvent)
    }

    @Test
    fun testSystemPackageDetectedEvent() {
        val event = LockEvent.SystemPackageDetected("com.android.systemui")
        assertEquals("com.android.systemui", event.packageName)
        assertTrue("Should be instance of LockEvent", event is LockEvent)
    }

    @Test
    fun testObjectEvents() {
        assertTrue("SettlementCompleted should be instance of LockEvent", 
            LockEvent.SettlementCompleted is LockEvent)
        assertTrue("CerberusAppOpened should be instance of LockEvent", 
            LockEvent.CerberusAppOpened is LockEvent)
        assertTrue("Reset should be instance of LockEvent", 
            LockEvent.Reset is LockEvent)
    }

    @Test
    fun testEventEquality() {
        val event1 = LockEvent.LockedAppOpened("com.example.app", "MainActivity")
        val event2 = LockEvent.LockedAppOpened("com.example.app", "MainActivity")
        val event3 = LockEvent.LockedAppOpened("com.example.app", "SettingsActivity")
        
        assertEquals("Same events should be equal", event1, event2)
        assertNotEquals("Different events should not be equal", event1, event3)
        
        // Test object events equality
        assertEquals("SettlementCompleted should equal itself", 
            LockEvent.SettlementCompleted, LockEvent.SettlementCompleted)
        assertEquals("CerberusAppOpened should equal itself", 
            LockEvent.CerberusAppOpened, LockEvent.CerberusAppOpened)
        assertEquals("Reset should equal itself", 
            LockEvent.Reset, LockEvent.Reset)
    }

    @Test
    fun testEventToString() {
        val event = LockEvent.LockedAppOpened("com.example.app", "MainActivity")
        val string = event.toString()
        assertTrue("ToString should contain package name", string.contains("com.example.app"))
        assertTrue("ToString should contain class name", string.contains("MainActivity"))
        assertTrue("ToString should contain event type", string.contains("LockedAppOpened"))
    }

    @Test
    fun testEventHashCode() {
        val event1 = LockEvent.LockedAppOpened("com.example.app", "MainActivity")
        val event2 = LockEvent.LockedAppOpened("com.example.app", "MainActivity")
        
        assertEquals("Equal events should have same hash code", event1.hashCode(), event2.hashCode())
    }

    @Test
    fun testAllEventTypesAreCovered() {
        // This test ensures we have all the event types we expect
        val events = listOf(
            LockEvent.LockedAppOpened("pkg", "cls"),
            LockEvent.NonLockedAppOpened("pkg"),
            LockEvent.SameAppActivityChanged("pkg", "cls"),
            LockEvent.SettlementCompleted,
            LockEvent.AuthenticationSucceeded("pkg"),
            LockEvent.AuthenticationFailed("pkg"),
            LockEvent.AppLeft("pkg"),
            LockEvent.SystemPackageDetected("pkg"),
            LockEvent.CerberusAppOpened,
            LockEvent.Reset
        )
        
        assertEquals("Should have 10 different event types", 10, events.size)
        events.forEach { event ->
            assertTrue("All events should be LockEvent instances", event is LockEvent)
        }
    }
}