package com.example.cerberus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import androidx.activity.ComponentActivity
import com.example.cerberus.data.AppInfoCache
import com.example.cerberus.data.LockedAppsCache
import com.example.cerberus.ui.AppListActivity
import com.example.cerberus.ui.AppListAdapter
import com.example.cerberus.ui.PermissionHelperActivity
import com.example.cerberus.utils.PermissionManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppInfoCache.preloadAll(this)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button_add_apps).setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }

        findViewById<Button>(R.id.save_secured_apps_button).setOnClickListener {
            val adapter = findViewById<ListView>(R.id.secured_apps_list_view).adapter as? AppListAdapter
            adapter?.let {
                LockedAppsCache.setLockedApps(this, it.getLockedApps())
                displaySecuredApps()
            }
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

        displaySecuredApps()
    }

    private fun displaySecuredApps() {
        val lockedApps = LockedAppsCache.getLockedApps(this).toMutableSet()
        val securedApps = lockedApps.mapNotNull { AppInfoCache.getAppInfo(this, it)
        }.sortedBy { it.appName.lowercase() }

        val listView = findViewById<ListView>(R.id.secured_apps_list_view)
        val adapter = AppListAdapter(this, securedApps, lockedApps)
        listView.adapter = adapter
    }


}
