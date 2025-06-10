package com.example.cerberus.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.*
import com.example.cerberus.R
import com.example.cerberus.data.SharedPreferencesUtil
import com.example.cerberus.model.AppInfo

class AppListActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isAccessibilityServiceEnabled(this) || !Settings.canDrawOverlays(this)) {
            val intent = Intent(this, PermissionHelperActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_app_list)

        val listView: ListView = findViewById(R.id.app_list_view)
        val pm = packageManager

        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map {
                AppInfo(
                    it.packageName,
                    pm.getApplicationLabel(it).toString(),
                    pm.getApplicationIcon(it)
                )
            }
            .sortedBy { it.appName.lowercase() }

        val lockedApps = SharedPreferencesUtil.getLockedApps(this).toMutableSet()
        val adapter = AppListAdapter(this, apps, lockedApps)
        listView.adapter = adapter

        val saveButton: Button = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            SharedPreferencesUtil.setLockedApps(this, adapter.getLockedApps())
            Toast.makeText(this, "Locked apps saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = "$packageName/com.example.cerberus.service.AppLockService"
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val colonSplitter = TextUtils.SimpleStringSplitter(':')

        if (enabledServicesSetting != null) {
            colonSplitter.setString(enabledServicesSetting)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }
}
