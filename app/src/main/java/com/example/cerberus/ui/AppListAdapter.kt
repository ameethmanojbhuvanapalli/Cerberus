package com.example.cerberus.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.cerberus.R
import com.example.cerberus.model.AppInfo

class AppListAdapter(
    private val context: Context,
    private val apps: List<AppInfo>,
    private val lockedApps: MutableSet<String>
) : BaseAdapter() {

    override fun getCount(): Int = apps.size

    override fun getItem(position: Int): Any = apps[position]

    override fun getItemId(position: Int): Long = position.toLong()

    fun getLockedApps(): Set<String> = lockedApps

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: ViewHolder
        val view: View

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_app, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = convertView.tag as ViewHolder
        }

        val app = apps[position]
        val isLocked = lockedApps.contains(app.packageName)

        // Set icon and name
        viewHolder.icon.setImageDrawable(app.appIcon)
        viewHolder.appName.text = app.appName

        // Set lock icon and card color
        if (isLocked) {
            viewHolder.lockIcon.setImageResource(R.drawable.ic_lock)
            viewHolder.card.setBackgroundResource(R.drawable.bg_gradient_dark_red)
        } else {
            viewHolder.lockIcon.setImageResource(R.drawable.ic_lock_open)
            viewHolder.card.setBackgroundResource(R.drawable.bg_gradient_black)
        }

        // Toggle lock state on click
        viewHolder.card.setOnClickListener {
            if (isLocked) {
                lockedApps.remove(app.packageName)
            } else {
                lockedApps.add(app.packageName)
            }
            notifyDataSetChanged()
        }

        return view
    }

    private class ViewHolder(view: View) {
        val card: CardView = view.findViewById(R.id.app_item_card)
        val icon: ImageView = view.findViewById(R.id.app_icon)
        val appName: TextView = view.findViewById(R.id.app_name)
        val lockIcon: ImageView = view.findViewById(R.id.lock_icon)
    }
}