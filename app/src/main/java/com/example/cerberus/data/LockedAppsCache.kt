package com.example.cerberus.data

import android.content.Context
import com.example.cerberus.utils.SharedPreferencesUtil

object LockedAppsCache {

    private var cachedLockedApps: Set<String>? = null

    fun getLockedApps(context: Context): Set<String> {
        if (cachedLockedApps == null) {
            cachedLockedApps = SharedPreferencesUtil.getLockedApps(context)
        }
        return cachedLockedApps ?: emptySet()
    }

    fun setLockedApps(context: Context, apps: Set<String>) {
        SharedPreferencesUtil.setLockedApps(context, apps)
        cachedLockedApps = apps
    }

    fun refresh(context: Context) {
        cachedLockedApps = SharedPreferencesUtil.getLockedApps(context)
    }

    fun clear() {
        cachedLockedApps = null
    }
}
