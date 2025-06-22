package com.example.cerberus.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.example.cerberus.R
import com.example.cerberus.data.AppInfoCache
import com.example.cerberus.data.LockedAppsCache

class AppListFragment : Fragment() {

    private var isLockedPage: Boolean = true
    private var adapter: AppListAdapter? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        arguments?.let {
            isLockedPage = it.getBoolean(ARG_IS_LOCKED, true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_app_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val listView: ListView = view.findViewById(R.id.app_list_view)
        val saveButton: Button = view.findViewById(R.id.save_button)
        val context = requireContext()
        val pm = context.packageManager
        val allLockedApps = LockedAppsCache.getLockedApps(context)
        val apps = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != context.packageName }
            .mapNotNull { AppInfoCache.getAppInfo(context, it.packageName) }
            .sortedBy { it.appName.lowercase() }

        val filteredApps = if (isLockedPage) {
            apps.filter { allLockedApps.contains(it.packageName) }
        } else {
            apps.filter { !allLockedApps.contains(it.packageName) }
        }

        val mutableLockedApps = allLockedApps.toMutableSet()
        adapter = AppListAdapter(context, filteredApps, mutableLockedApps)
        listView.adapter = adapter

        saveButton.setOnClickListener {
            adapter?.let {
                LockedAppsCache.setLockedApps(context, it.getLockedApps())
                // Optionally show a toast or update UI
            }
        }
    }

    companion object {
        private const val ARG_IS_LOCKED = "isLocked"
        fun newInstance(isLocked: Boolean): AppListFragment {
            val fragment = AppListFragment()
            fragment.arguments = Bundle().apply {
                putBoolean(ARG_IS_LOCKED, isLocked)
            }
            return fragment
        }
    }
}