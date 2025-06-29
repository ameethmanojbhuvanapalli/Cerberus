package com.example.cerberus

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.cerberus.auth.AuthenticatorType
import com.example.cerberus.data.AuthenticatorTypeCache
import com.example.cerberus.ui.AppListTabsActivity
import com.example.cerberus.ui.AuthSettingsActivity
import com.example.cerberus.ui.PermissionHelperFragment
import com.example.cerberus.utils.PermissionManager
import com.example.cerberus.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.preloadAppInfo(this)

        binding.appLockCard.apply {
            setTitle(getString(R.string.app_lock))
            setDescription(getString(R.string.secure_your_apps))
            setPillText(getString(R.string.secured_apps_text, 0))
            setIcon(R.drawable.ic_lock_fill)
            setIconContentDescription(getString(R.string.app_lock))
            setOnClickListener {
                startActivity(Intent(context, AppListTabsActivity::class.java))
            }
        }

        binding.authSettingsCard.apply {
            setTitle(getString(R.string.auth_settings))
            setDescription(getString(R.string.auth_settings_desc))
            setPillText(getString(R.string.auth_enabled_text, AuthenticatorTypeCache.getAuthenticatorType(this@MainActivity).name))
            setIcon(R.drawable.ic_auth_settings)
            setIconContentDescription(getString(R.string.auth_settings))
            setOnClickListener {
                startActivity(Intent(context, AuthSettingsActivity::class.java))
            }
        }

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
            showPermissionFragment()
        } else {
            removePermissionFragmentIfPresent()
            viewModel.updateLockedAppsCount(this)
            val currentType = AuthenticatorTypeCache.getAuthenticatorType(this)
            val typeName = when (currentType) {
                AuthenticatorType.BIOMETRIC -> getString(R.string.biometric)
                AuthenticatorType.PIN -> getString(R.string.pin)
                AuthenticatorType.PATTERN -> getString(R.string.pattern)
                AuthenticatorType.PASSWORD -> getString(R.string.password)
            }
            binding.authSettingsCard.setPillText(getString(R.string.auth_enabled_text, typeName))
        }
    }

    private fun showPermissionFragment() {
        // Avoid multiple fragments
        if (supportFragmentManager.findFragmentByTag("perm_fragment") == null) {
            val fragment = PermissionHelperFragment {
                viewModel.updateLockedAppsCount(this)
            }
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, fragment, "perm_fragment")
                .commitAllowingStateLoss()
        }
    }

    private fun removePermissionFragmentIfPresent() {
        val frag = supportFragmentManager.findFragmentByTag("perm_fragment")
        if (frag != null) {
            supportFragmentManager.beginTransaction().remove(frag).commitAllowingStateLoss()
        }
    }
}