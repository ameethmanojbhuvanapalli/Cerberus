package com.example.cerberus

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.cerberus.ui.AppListTabsActivity
import com.example.cerberus.ui.PermissionHelperDialogFragment
import com.example.cerberus.utils.PermissionManager
import com.example.cerberus.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Preload app info in ViewModel
        viewModel.preloadAppInfo(this)

        binding.appLockCard.setOnClickListener {
            startActivity(Intent(this, AppListTabsActivity::class.java))
        }

        // Observe locked apps count
        viewModel.lockedAppsCount.observe(this, Observer { count ->
            val securedAppsText = getString(R.string.secured_apps_text, count)
            binding.homeAppsSecured.text = securedAppsText
        })
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndUpdateUI()
    }

    private fun checkPermissionsAndUpdateUI() {
        if (!PermissionManager.hasOverlayPermission(this) ||
            !PermissionManager.hasAccessibilityPermission(this)
        ) {
            val dialogFragment = PermissionHelperDialogFragment()
            dialogFragment.setPermissionGrantedCallback {
                viewModel.updateLockedAppsCount(this)
            }
            dialogFragment.show(supportFragmentManager, "perm_dialog")
        } else {
            viewModel.updateLockedAppsCount(this)
        }
    }
}