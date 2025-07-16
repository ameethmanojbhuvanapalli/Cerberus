package com.example.cerberus.applock.statemachine

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for the SystemPackageFilter to ensure comprehensive OEM coverage
 */
class SystemPackageFilterTest {

    @Test
    fun testAndroidSystemPackages() {
        assertTrue("Should detect android package", SystemPackageFilter.isSystemPackage("android"))
        assertTrue("Should detect systemui", SystemPackageFilter.isSystemPackage("com.android.systemui"))
        assertTrue("Should detect launcher", SystemPackageFilter.isSystemPackage("com.android.launcher"))
        assertTrue("Should detect settings", SystemPackageFilter.isSystemPackage("com.android.settings"))
        assertTrue("Should detect null package", SystemPackageFilter.isSystemPackage(null))
    }

    @Test
    fun testSamsungSystemPackages() {
        assertTrue("Should detect Samsung launcher", SystemPackageFilter.isSystemPackage("com.samsung.android.launcher"))
        assertTrue("Should detect Samsung One UI", SystemPackageFilter.isSystemPackage("com.samsung.android.oneui.home"))
        assertTrue("Should detect Samsung Knox", SystemPackageFilter.isSystemPackage("com.samsung.knox"))
        assertTrue("Should detect Samsung Bixby", SystemPackageFilter.isSystemPackage("com.samsung.android.bixby.agent"))
    }

    @Test
    fun testXiaomiSystemPackages() {
        assertTrue("Should detect MIUI home", SystemPackageFilter.isSystemPackage("com.miui.home"))
        assertTrue("Should detect MIUI launcher", SystemPackageFilter.isSystemPackage("com.miui.launcher"))
        assertTrue("Should detect MIUI security", SystemPackageFilter.isSystemPackage("com.miui.securitycenter"))
        assertTrue("Should detect Xiaomi scanner", SystemPackageFilter.isSystemPackage("com.xiaomi.scanner"))
    }

    @Test
    fun testOppoSystemPackages() {
        assertTrue("Should detect Oppo launcher", SystemPackageFilter.isSystemPackage("com.oppo.launcher"))
        assertTrue("Should detect ColorOS launcher", SystemPackageFilter.isSystemPackage("com.coloros.launcher"))
        assertTrue("Should detect Oppo safe", SystemPackageFilter.isSystemPackage("com.oppo.safe"))
        assertTrue("Should detect ColorOS safe center", SystemPackageFilter.isSystemPackage("com.coloros.safecenter"))
    }

    @Test
    fun testOnePlusSystemPackages() {
        assertTrue("Should detect OnePlus launcher", SystemPackageFilter.isSystemPackage("net.oneplus.launcher"))
        assertTrue("Should detect OnePlus security", SystemPackageFilter.isSystemPackage("com.oneplus.security"))
        assertTrue("Should detect OnePlus gallery", SystemPackageFilter.isSystemPackage("com.oneplus.gallery"))
    }

    @Test
    fun testVivoSystemPackages() {
        assertTrue("Should detect Vivo launcher", SystemPackageFilter.isSystemPackage("com.vivo.launcher"))
        assertTrue("Should detect BBK launcher", SystemPackageFilter.isSystemPackage("com.bbk.launcher2"))
        assertTrue("Should detect Vivo safe center", SystemPackageFilter.isSystemPackage("com.vivo.safecenter"))
    }

    @Test
    fun testGestureAnimationPackages() {
        assertTrue("Should detect gesture navigation", SystemPackageFilter.isGestureAnimation("gesture_navigation"))
        assertTrue("Should detect launcher transition", SystemPackageFilter.isGestureAnimation("launcher_transition"))
        assertTrue("Should detect system animation", SystemPackageFilter.isGestureAnimation("system_animation"))
        assertTrue("Should detect packages with transition", SystemPackageFilter.isGestureAnimation("com.android.transition.test"))
        assertTrue("Should detect null gesture", SystemPackageFilter.isGestureAnimation(null))
    }

    @Test
    fun testNonSystemPackages() {
        assertFalse("Should not detect regular app", SystemPackageFilter.isSystemPackage("com.example.myapp"))
        assertFalse("Should not detect third party launcher", SystemPackageFilter.isSystemPackage("com.nova.launcher"))
        assertFalse("Should not detect social media app", SystemPackageFilter.isSystemPackage("com.facebook.katana"))
        assertFalse("Should not detect game", SystemPackageFilter.isSystemPackage("com.supercell.clashofclans"))
    }

    @Test
    fun testPatternBasedMatching() {
        assertTrue("Should detect android subpackage", SystemPackageFilter.isSystemPackage("com.android.providers.contacts"))
        assertTrue("Should detect google android subpackage", SystemPackageFilter.isSystemPackage("com.google.android.gms.auth"))
        assertTrue("Should detect samsung android subpackage", SystemPackageFilter.isSystemPackage("com.samsung.android.app.notes"))
        assertTrue("Should detect miui subpackage", SystemPackageFilter.isSystemPackage("com.miui.powerkeeper"))
    }

    @Test
    fun testEdgeCases() {
        assertTrue("Should handle empty string as system", SystemPackageFilter.isSystemPackage(""))
        assertFalse("Should not detect partial match", SystemPackageFilter.isSystemPackage("myapp.com.android.fake"))
        assertFalse("Should not detect case mismatch", SystemPackageFilter.isSystemPackage("COM.ANDROID.SYSTEMUI"))
    }
}