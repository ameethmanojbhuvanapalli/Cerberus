package com.example.cerberus.ui

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import com.example.cerberus.R
import com.example.cerberus.data.SharedPreferencesUtil
import com.example.cerberus.model.AppInfo

class AppListActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        val listView: ListView = findViewById(R.id.app_list_view)
        val pm = packageManager
        val lockedApps = SharedPreferencesUtil.getLockedApps(this)

        val unprotectedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .filter { it.packageName !in lockedApps }
            .map {
                AppInfo(
                    it.packageName,
                    pm.getApplicationLabel(it).toString(),
                    pm.getApplicationIcon(it)
                )
            }
            .sortedBy { it.appName.lowercase() }

        val newLockedSet = lockedApps.toMutableSet()
        val adapter = AppListAdapter(this, unprotectedApps, newLockedSet)
        listView.adapter = adapter

        val saveButton: Button = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            SharedPreferencesUtil.setLockedApps(this, adapter.getLockedApps())
            setResult(RESULT_OK)
            finish()
        }
    }
}
