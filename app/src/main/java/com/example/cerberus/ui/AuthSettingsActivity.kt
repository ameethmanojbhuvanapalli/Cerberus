package com.example.cerberus.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.cerberus.R
import com.example.cerberus.auth.AuthenticatorType
import com.example.cerberus.data.AuthenticatorTypeCache
import com.example.cerberus.databinding.ActivityAuthSettingsBinding

class AuthSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set currently selected type
        val currentType = AuthenticatorTypeCache.getAuthenticatorType(this)
        when (currentType) {
            AuthenticatorType.BIOMETRIC -> binding.radioBiometric.isChecked = true
            AuthenticatorType.PIN -> binding.radioPin.isChecked = true
            AuthenticatorType.PATTERN -> binding.radioPattern.isChecked = true
        }

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val type = when (checkedId) {
                R.id.radioBiometric -> AuthenticatorType.BIOMETRIC
                R.id.radioPin -> AuthenticatorType.PIN
                R.id.radioPattern -> AuthenticatorType.PATTERN
                else -> AuthenticatorType.BIOMETRIC
            }
            AuthenticatorTypeCache.setAuthenticatorType(this, type)
        }
    }
}