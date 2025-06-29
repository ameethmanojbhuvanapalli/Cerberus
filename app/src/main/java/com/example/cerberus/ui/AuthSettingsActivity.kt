package com.example.cerberus.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cerberus.R
import com.example.cerberus.auth.AuthenticationManager
import com.example.cerberus.auth.AuthenticatorType
import com.example.cerberus.data.*
import com.example.cerberus.databinding.ActivityAuthSettingsBinding
import com.example.cerberus.model.AuthenticatorTypeItem
import com.example.cerberus.service.AuthenticationService

class AuthSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthSettingsBinding
    private lateinit var adapter: AuthTypeSelectAdapter
    private lateinit var authTypes: List<AuthenticatorTypeItem>
    private var selectedType: AuthenticatorTypeItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authTypes = listOf(
            AuthenticatorTypeItem(
                AuthenticatorType.BIOMETRIC,
                R.drawable.ic_fingerprint,
                getString(R.string.biometric)
            ),
            AuthenticatorTypeItem(
                AuthenticatorType.PIN,
                R.drawable.ic_pin,
                getString(R.string.pin)
            ),
            AuthenticatorTypeItem(
                AuthenticatorType.PATTERN,
                R.drawable.ic_pattern,
                getString(R.string.pattern)
            ),
            AuthenticatorTypeItem(
                AuthenticatorType.PASSWORD,
                R.drawable.ic_password,
                getString(R.string.password)
            )
        )

        val cachedType = AuthenticatorTypeCache.getAuthenticatorType(this)
        selectedType = authTypes.find { it.type == cachedType } ?: authTypes.first()

        adapter = AuthTypeSelectAdapter(this, authTypes, selectedType!!) { selected ->
            selectedType = selected
            updateCredentialButton()
            updateSaveButtonState()
        }

        binding.authTypeListView.adapter = adapter

        binding.setCredentialButton.setOnClickListener {
            when (selectedType?.type) {
                AuthenticatorType.PIN -> {
                    val fragment = PinSetupFragment().apply {
                        onPinSet = {
                            updateSaveButtonState()
                            Toast.makeText(context, "PIN set", Toast.LENGTH_SHORT).show()
                        }
                    }
                    fragment.show(supportFragmentManager, "pin_setup")
                }
                AuthenticatorType.PATTERN -> {
                    val frag = PatternSetupFragment().apply {
                        onPatternSet = {
                            updateSaveButtonState()
                            Toast.makeText(this@AuthSettingsActivity, "Pattern set", Toast.LENGTH_SHORT).show()
                        }
                    }
                    frag.show(supportFragmentManager, "pattern_setup")
                }
                AuthenticatorType.PASSWORD -> {
                    val fragment = PasswordSetupFragment().apply {
                        onPasswordSet = {
                            updateSaveButtonState()
                            Toast.makeText(this@AuthSettingsActivity, "Password set", Toast.LENGTH_SHORT).show()
                        }
                    }
                    fragment.show(supportFragmentManager, "password_setup")
                }
                else -> {}
            }
        }

        binding.setIdleTimeoutButton.setOnClickListener {
            IdleTimeoutPickerFragment { newTimeout ->
                IdleTimeoutCache.setIdleTimeout(this, newTimeout)
                AuthenticationService.getInstance(this).clearAuthenticatedApps()
                Toast.makeText(this, "Timeout updated", Toast.LENGTH_SHORT).show()
            }.apply {
                show(supportFragmentManager, "idle_timeout_picker")
            }
        }

        binding.saveButton.setOnClickListener {
            val durationMs = IdleTimeoutCache.getIdleTimeout(this)
            AuthenticationService.getInstance(this).clearAuthenticatedApps()
            selectedType?.let {
                AuthenticatorTypeCache.setAuthenticatorType(this, it.type)
                AuthenticationManager.getInstance(this).setAuthenticatorType(it.type)
                Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        updateCredentialButton()
        updateSaveButtonState()
    }

    private fun updateCredentialButton() {
        when (selectedType?.type) {
            AuthenticatorType.PIN -> {
                binding.setCredentialButton.visibility = android.view.View.VISIBLE
                binding.setCredentialButton.setText(R.string.set_pin)
                binding.setCredentialButton.setIconResource(R.drawable.ic_pin)
            }
            AuthenticatorType.PATTERN -> {
                binding.setCredentialButton.visibility = android.view.View.VISIBLE
                binding.setCredentialButton.setText(R.string.set_pattern)
                binding.setCredentialButton.setIconResource(R.drawable.ic_pattern)
            }
            AuthenticatorType.PASSWORD -> {
                binding.setCredentialButton.visibility = android.view.View.VISIBLE
                binding.setCredentialButton.setText(R.string.set_password)
                binding.setCredentialButton.setIconResource(R.drawable.ic_password)
            }
            else -> {
                binding.setCredentialButton.visibility = android.view.View.GONE
            }
        }
    }

    private fun updateSaveButtonState() {
        binding.saveButton.isEnabled = when (selectedType?.type) {
            AuthenticatorType.PIN -> PinCache.hasPin(this)
            AuthenticatorType.PATTERN -> PatternCache.hasPattern(this)
            AuthenticatorType.PASSWORD -> PasswordCache.hasPassword(this)
            AuthenticatorType.BIOMETRIC -> true
            else -> false
        }
    }
}
