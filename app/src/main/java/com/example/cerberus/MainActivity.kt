package com.example.cerberus

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

        // App Lock Card
        binding.appLockCard.apply {
            setTitle(getString(R.string.app_lock))
            setDescription(getString(R.string.secure_your_apps))
            setPillText(getString(R.string.secured_apps_text, 0)) // will update via observer
            setIcon(R.drawable.ic_lock_fill)
            setIconContentDescription(getString(R.string.app_lock))
            setOnClickListener {
                startActivity(Intent(context, AppListTabsActivity::class.java))
            }
        }

        // Observe locked apps count for appLockCard
        viewModel.lockedAppsCount.observe(this) { count ->
            binding.appLockCard.setPillText(getString(R.string.secured_apps_text, count))
        }
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