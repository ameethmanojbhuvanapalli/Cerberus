package com.example.cerberus.applock.statemachine

/**
 * Universal system package filter that detects system packages across all Android OEMs.
 * This prevents false app switch detection when users interact with system UI elements,
 * gesture navigation, and OEM-specific system applications.
 */
object SystemPackageFilter {
    
    /**
     * Core Android system packages present on all devices
     */
    private val androidSystemPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher2", 
        "com.android.launcher3",
        "com.android.settings",
        "com.android.inputmethod.latin",
        "com.android.shell",
        "com.android.phone",
        "com.android.dialer",
        "com.android.contacts",
        "com.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.gms",
        "com.google.android.gsf",
        null // Null package names from system events
    )
    
    /**
     * Samsung system packages (One UI)
     */
    private val samsungSystemPackages = setOf(
        "com.samsung.android.launcher",
        "com.samsung.android.app.launcher",
        "com.sec.android.app.launcher",
        "com.samsung.android.honeyboard",
        "com.samsung.android.bixby.agent",
        "com.samsung.android.app.cocktailbarservice",
        "com.samsung.android.goodlock",
        "com.samsung.android.game.gametools",
        "com.samsung.android.oneui.home",
        "com.samsung.systemui",
        "com.samsung.knox"
    )
    
    /**
     * Xiaomi system packages (MIUI)
     */
    private val xiaomiSystemPackages = setOf(
        "com.miui.home",
        "com.miui.launcher",
        "com.miui.systemui.plugin",
        "com.miui.securitycenter",
        "com.xiaomi.scanner",
        "com.miui.gallery",
        "com.miui.powerkeeper",
        "com.miui.guardprovider",
        "com.miui.backup",
        "com.xiaomi.market"
    )
    
    /**
     * Oppo system packages (ColorOS)
     */
    private val oppoSystemPackages = setOf(
        "com.oppo.launcher",
        "com.coloros.launcher",
        "com.oppo.safe",
        "com.coloros.safecenter", 
        "com.oppo.gamecenter",
        "com.coloros.gamecenter",
        "com.oppo.music",
        "com.coloros.gallery3d",
        "com.oppo.gallery3d"
    )
    
    /**
     * OnePlus system packages (OxygenOS)
     */
    private val onePlusSystemPackages = setOf(
        "net.oneplus.launcher",
        "com.oneplus.launcher",
        "com.oneplus.security",
        "com.oneplus.gallery",
        "com.oneplus.gamespace",
        "com.oneplus.setupwizard",
        "com.android.launcher",
        "com.oneplus.filemanager"
    )
    
    /**
     * Vivo system packages (FunTouch OS/Origin OS)
     */
    private val vivoSystemPackages = setOf(
        "com.vivo.launcher",
        "com.bbk.launcher2",
        "com.vivo.securedaemonservice",
        "com.vivo.safecenter",
        "com.vivo.gallery",
        "com.vivo.gamecenter",
        "com.vivo.space",
        "com.bbk.appstore"
    )
    
    /**
     * Gesture navigation and animation packages that appear briefly during transitions
     */
    private val gestureAnimationPackages = setOf(
        "com.android.systemui.navigation",
        "com.android.systemui.gesture",
        "launcher_transition",
        "gesture_navigation",
        "system_animation"
    )
    
    /**
     * All system packages combined for efficient lookup
     */
    private val allSystemPackages: Set<String?> by lazy {
        androidSystemPackages + samsungSystemPackages + xiaomiSystemPackages +
        oppoSystemPackages + onePlusSystemPackages + vivoSystemPackages +
        gestureAnimationPackages
    }
    
    /**
     * Checks if the given package name is a system package that should be ignored.
     * 
     * @param packageName The package name to check (can be null)
     * @return true if this is a system package that should be ignored, false otherwise
     */
    fun isSystemPackage(packageName: String?): Boolean {
        if (packageName == null) return true
        
        // Direct lookup in combined set
        if (allSystemPackages.contains(packageName)) return true
        
        // Pattern-based matching for packages that may have version suffixes or variations
        return isSystemPackageByPattern(packageName)
    }
    
    /**
     * Pattern-based matching for system packages that may have variations
     */
    private fun isSystemPackageByPattern(packageName: String): Boolean {
        val systemPatterns = listOf(
            "com.android.",
            "com.google.android.",
            "com.samsung.android.",
            "com.miui.",
            "com.xiaomi.",
            "com.oppo.",
            "com.coloros.",
            "com.oneplus.",
            "com.vivo.",
            "com.bbk.",
            "com.sec.android."
        )
        
        return systemPatterns.any { pattern ->
            packageName.startsWith(pattern) && isLikelySystemComponent(packageName)
        }
    }
    
    /**
     * Additional heuristics to identify system components
     */
    private fun isLikelySystemComponent(packageName: String): Boolean {
        val systemKeywords = listOf(
            "launcher", "systemui", "settings", "dialer", "contacts",
            "keyboard", "ime", "inputmethod", "permission", "package",
            "system", "framework", "service", "provider", "knox",
            "secure", "safe", "guard", "gesture", "navigation"
        )
        
        return systemKeywords.any { keyword ->
            packageName.lowercase().contains(keyword)
        }
    }
    
    /**
     * Checks if a package might be a brief gesture animation or transition effect
     * that should be ignored to prevent false app switch detection.
     */
    fun isGestureAnimation(packageName: String?): Boolean {
        if (packageName == null) return true
        
        return gestureAnimationPackages.contains(packageName) ||
               packageName.contains("transition") ||
               packageName.contains("animation") ||
               packageName.contains("gesture")
    }
}