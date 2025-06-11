package com.example.cerberus

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.cerberus.data.SharedPreferencesUtil
import com.example.cerberus.model.AppInfo
import com.example.cerberus.ui.AppListActivity
import com.example.cerberus.ui.PermissionHelperActivity
import com.example.cerberus.utils.PermissionManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button_add_apps).setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
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
        val container = findViewById<LinearLayout>(R.id.secured_apps_container)
        container.removeAllViews()

        val lockedApps = SharedPreferencesUtil.getLockedApps(this)
        val pm = packageManager

        val securedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName in lockedApps }
            .map {
                AppInfo(
                    it.packageName,
                    pm.getApplicationLabel(it).toString(),
                    pm.getApplicationIcon(it)
                )
            }

        for (app in securedApps) {
            val appView = TextView(this).apply {
                text = app.appName
                setPadding(16, 16, 16, 16)
                textSize = 16f
            }
            container.addView(appView)
        }
    }
}
