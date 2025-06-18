package com.example.cerberus.data

import android.content.Context
import android.content.pm.PackageManager
import com.example.cerberus.model.AppInfo

object AppInfoCache {

    private val cache = mutableMapOf<String, AppInfo>()

    fun getAppInfo(context: Context, packageName: String): AppInfo? {
        if (cache.containsKey(packageName)) return cache[packageName]

        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val app = AppInfo(
                packageName,
                pm.getApplicationLabel(appInfo).toString(),
                pm.getApplicationIcon(appInfo)
            )
            cache[packageName] = app
            app
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun preloadAll(context: Context) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        apps.forEach {
            if (pm.getLaunchIntentForPackage(it.packageName) != null) {
                getAppInfo(context, it.packageName)
            }
        }
    }

    fun clear() = cache.clear()
}
