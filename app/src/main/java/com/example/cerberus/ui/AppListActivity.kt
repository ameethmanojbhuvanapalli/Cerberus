package com.example.cerberus.ui

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.*
import com.example.cerberus.R
import com.example.cerberus.data.SharedPreferencesUtil

class AppListActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasUsageStatsPermission(this) || !Settings.canDrawOverlays(this)) {
            val intent = Intent(this, PermissionHelperActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_app_list)

        val listView: ListView = findViewById(R.id.app_list_view)
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { it.packageName }
            .sorted()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, apps)
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        val lockedApps = SharedPreferencesUtil.getLockedApps(this)
        apps.forEachIndexed { index, app ->
            if (lockedApps.contains(app)) listView.setItemChecked(index, true)
        }

        val saveButton: Button = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            val selectedApps = mutableSetOf<String>()
            for (i in 0 until listView.count) {
                if (listView.isItemChecked(i)) selectedApps.add(apps[i])
            }
            SharedPreferencesUtil.setLockedApps(this, selectedApps)
            Toast.makeText(this, "Locked apps saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
