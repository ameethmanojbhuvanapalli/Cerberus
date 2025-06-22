package com.example.cerberus

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.cardview.widget.CardView
import com.example.cerberus.data.AppInfoCache
import com.example.cerberus.data.LockedAppsCache
import com.example.cerberus.ui.AppListTabsActivity
import com.example.cerberus.ui.PermissionHelperActivity
import com.example.cerberus.utils.PermissionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppInfoCache.preloadAll(this)
        setContentView(R.layout.activity_main)

        findViewById<CardView>(R.id.app_lock_card).setOnClickListener {
            startActivity(Intent(this, AppListTabsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (!PermissionManager.hasOverlayPermission(this) ||
            !PermissionManager.hasAccessibilityPermission(this)
        ) {
            startActivity(Intent(this, PermissionHelperActivity::class.java))
            finish()
            return
        }
        updateSecuredAppsCount()
    }

    private fun updateSecuredAppsCount() {
        val lockedAppsCount = LockedAppsCache.getLockedApps(this).size
        val securedAppsText = getString(R.string.secured_apps_text, lockedAppsCount)
        findViewById<TextView>(R.id.home_apps_secured).text = securedAppsText
    }
}