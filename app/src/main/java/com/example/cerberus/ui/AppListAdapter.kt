package com.example.cerberus.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.cerberus.R
import com.example.cerberus.model.AppInfo
import com.google.android.material.switchmaterial.SwitchMaterial

class AppListAdapter(
    context: Context,
    private val apps: List<AppInfo>,
    private val lockedApps: MutableSet<String>
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = apps.size
    override fun getItem(position: Int): Any = apps[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.list_item_app, parent, false)

        val appIcon = view.findViewById<ImageView>(R.id.app_icon)
        val appName = view.findViewById<TextView>(R.id.app_name)
        val appSwitch = view.findViewById<SwitchMaterial>(R.id.app_switch)

        val app = apps[position]

        appIcon.setImageDrawable(app.appIcon)
        appName.text = app.appName
        appSwitch.setOnCheckedChangeListener(null)
        appSwitch.isChecked = lockedApps.contains(app.packageName)

        appSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) lockedApps.add(app.packageName)
            else lockedApps.remove(app.packageName)
        }

        return view
    }

    fun getLockedApps(): Set<String> = lockedApps
}
