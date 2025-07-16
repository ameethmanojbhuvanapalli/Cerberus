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
    fun testMotorolaSystemPackages() {
        assertTrue("Should detect Motorola launcher", SystemPackageFilter.isSystemPackage("com.motorola.launcher3"))
        assertTrue("Should detect Motorola actions", SystemPackageFilter.isSystemPackage("com.motorola.actions"))
        assertTrue("Should detect Motorola display", SystemPackageFilter.isSystemPackage("com.motorola.display"))
        assertTrue("Should detect Motorola camera", SystemPackageFilter.isSystemPackage("com.motorola.camera"))
    }

    @Test
    fun testHuaweiSystemPackages() {
        assertTrue("Should detect Huawei launcher", SystemPackageFilter.isSystemPackage("com.huawei.android.launcher"))
        assertTrue("Should detect Huawei systemui", SystemPackageFilter.isSystemPackage("com.huawei.systemui"))
        assertTrue("Should detect Honor launcher", SystemPackageFilter.isSystemPackage("com.hihonor.android.launcher"))
        assertTrue("Should detect Huawei intelligent", SystemPackageFilter.isSystemPackage("com.huawei.intelligent"))
    }

    @Test
    fun testRealmeSystemPackages() {
        assertTrue("Should detect Realme launcher", SystemPackageFilter.isSystemPackage("com.realme.launcher"))
        assertTrue("Should detect Realme setup", SystemPackageFilter.isSystemPackage("com.realme.setupwizard"))
        assertTrue("Should detect Realme safe", SystemPackageFilter.isSystemPackage("com.realme.safe"))
    }

    @Test
    fun testNothingSystemPackages() {
        assertTrue("Should detect Nothing launcher", SystemPackageFilter.isSystemPackage("com.nothing.launcher"))
        assertTrue("Should detect Nothing systemui", SystemPackageFilter.isSystemPackage("com.nothing.systemui"))
        assertTrue("Should detect Nothing ketchum", SystemPackageFilter.isSystemPackage("com.nothing.ketchum"))
    }

    @Test
    fun testNokiaSystemPackages() {
        assertTrue("Should detect Nokia launcher", SystemPackageFilter.isSystemPackage("com.hmdglobal.launcher3"))
        assertTrue("Should detect Nokia setup", SystemPackageFilter.isSystemPackage("com.hmdglobal.setup"))
        assertTrue("Should detect Nokia camera", SystemPackageFilter.isSystemPackage("com.hmdglobal.camera"))
    }

    @Test
    fun testSonySystemPackages() {
        assertTrue("Should detect Sony launcher", SystemPackageFilter.isSystemPackage("com.sonymobile.launcher"))
        assertTrue("Should detect Sony Xperia lounge", SystemPackageFilter.isSystemPackage("com.sonymobile.xperialounge"))
        assertTrue("Should detect Sony assist", SystemPackageFilter.isSystemPackage("com.sonymobile.assist"))
    }

    @Test
    fun testLgSystemPackages() {
        assertTrue("Should detect LG launcher2", SystemPackageFilter.isSystemPackage("com.lge.launcher2"))
        assertTrue("Should detect LG launcher3", SystemPackageFilter.isSystemPackage("com.lge.launcher3"))
        assertTrue("Should detect LG systemui", SystemPackageFilter.isSystemPackage("com.lge.systemui"))
    }

    @Test
    fun testHtcSystemPackages() {
        assertTrue("Should detect HTC launcher", SystemPackageFilter.isSystemPackage("com.htc.launcher"))
        assertTrue("Should detect HTC Sense launcher", SystemPackageFilter.isSystemPackage("com.htc.sense.launcher"))
        assertTrue("Should detect HTC camera", SystemPackageFilter.isSystemPackage("com.htc.camera"))
    }

    @Test
    fun testMiscOemSystemPackages() {
        // Asus
        assertTrue("Should detect Asus launcher", SystemPackageFilter.isSystemPackage("com.asus.launcher"))
        assertTrue("Should detect Asus ZenUI", SystemPackageFilter.isSystemPackage("com.asus.zenui"))
        
        // Tecno
        assertTrue("Should detect Tecno launcher", SystemPackageFilter.isSystemPackage("com.transsion.launcher"))
        assertTrue("Should detect Tecno systemui", SystemPackageFilter.isSystemPackage("com.transsion.systemui"))
        
        // Infinix
        assertTrue("Should detect Infinix launcher", SystemPackageFilter.isSystemPackage("com.infinix.launcher"))
        
        // Meizu
        assertTrue("Should detect Meizu launcher", SystemPackageFilter.isSystemPackage("com.meizu.flyme.launcher"))
        assertTrue("Should detect Meizu gallery", SystemPackageFilter.isSystemPackage("com.meizu.media.gallery"))
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
        
        // Test new manufacturer patterns
        assertTrue("Should detect motorola subpackage", SystemPackageFilter.isSystemPackage("com.motorola.launcher.test"))
        assertTrue("Should detect huawei subpackage", SystemPackageFilter.isSystemPackage("com.huawei.android.test"))
        assertTrue("Should detect honor subpackage", SystemPackageFilter.isSystemPackage("com.hihonor.android.test"))
        assertTrue("Should detect realme subpackage", SystemPackageFilter.isSystemPackage("com.realme.test.app"))
        assertTrue("Should detect nothing subpackage", SystemPackageFilter.isSystemPackage("com.nothing.test.service"))
        assertTrue("Should detect hmd subpackage", SystemPackageFilter.isSystemPackage("com.hmdglobal.test.app"))
        assertTrue("Should detect sony subpackage", SystemPackageFilter.isSystemPackage("com.sonymobile.test.service"))
        assertTrue("Should detect lge subpackage", SystemPackageFilter.isSystemPackage("com.lge.test.launcher"))
        assertTrue("Should detect htc subpackage", SystemPackageFilter.isSystemPackage("com.htc.test.sense"))
        assertTrue("Should detect asus subpackage", SystemPackageFilter.isSystemPackage("com.asus.test.zen"))
        assertTrue("Should detect transsion subpackage", SystemPackageFilter.isSystemPackage("com.transsion.test.hios"))
        assertTrue("Should detect meizu subpackage", SystemPackageFilter.isSystemPackage("com.meizu.test.flyme"))
    }

    @Test
    fun testEdgeCases() {
        assertTrue("Should handle empty string as system", SystemPackageFilter.isSystemPackage(""))
        assertFalse("Should not detect partial match", SystemPackageFilter.isSystemPackage("myapp.com.android.fake"))
        assertFalse("Should not detect case mismatch", SystemPackageFilter.isSystemPackage("COM.ANDROID.SYSTEMUI"))
    }
}