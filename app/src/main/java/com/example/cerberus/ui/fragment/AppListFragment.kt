package com.example.cerberus.ui.fragment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.example.cerberus.R
import com.example.cerberus.data.AppInfoCache
import com.example.cerberus.data.LockedAppsCache
import com.example.cerberus.ui.activity.AppListTabsActivity
import com.example.cerberus.ui.adapter.AppListAdapter

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
        val searchEditText: EditText = view.findViewById(R.id.search_edit_text)
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

        // Set up search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter?.filter(s?.toString() ?: "")
            }
        })

        saveButton.setOnClickListener {
            adapter?.let {
                LockedAppsCache.setLockedApps(context, it.getLockedApps())
                (activity as? AppListTabsActivity)?.refreshAllTabs()
                android.widget.Toast.makeText(context, R.string.saved, android.widget.Toast.LENGTH_SHORT).show()
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

    fun refreshList() {
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

        adapter = AppListAdapter(context, filteredApps, allLockedApps.toMutableSet())
        val listView: ListView = view?.findViewById(R.id.app_list_view) ?: return
        val searchEditText: EditText? = view?.findViewById(R.id.search_edit_text)
        listView.adapter = adapter
        
        // Re-apply search filter if there's text in the search box
        searchEditText?.text?.toString()?.let { searchText ->
            if (searchText.isNotEmpty()) {
                adapter?.filter(searchText)
            }
        }
    }
}