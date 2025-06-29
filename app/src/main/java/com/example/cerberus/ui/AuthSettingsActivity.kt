package com.example.cerberus.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cerberus.R
import com.example.cerberus.auth.AuthenticationManager
import com.example.cerberus.auth.AuthenticatorType
import com.example.cerberus.data.AuthenticatorTypeCache
import com.example.cerberus.data.IdleTimeoutCache
import com.example.cerberus.data.PatternCache
import com.example.cerberus.data.PinCache
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
                else -> {}
            }
        }

        setupDurationPickers()

        binding.saveButton.setOnClickListener {
            val durationMs = getSelectedDurationMs()
            IdleTimeoutCache.setIdleTimeout(this,durationMs)
            AuthenticationService.getInstance(this).clearAuthenticatedApps()
            Toast.makeText(this, "Duration: $durationMs ms saved", Toast.LENGTH_SHORT).show()

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

    private fun setupDurationPickers() {
        binding.npHours.minValue = 0
        binding.npHours.maxValue = 23
        binding.npHours.wrapSelectorWheel = true

        binding.npMinutes.minValue = 0
        binding.npMinutes.maxValue = 59
        binding.npMinutes.wrapSelectorWheel = true

        binding.npSeconds.minValue = 0
        binding.npSeconds.maxValue = 59
        binding.npSeconds.wrapSelectorWheel = true
    }

    private fun getSelectedDurationMs(): Long {
        val hours = binding.npHours.value
        val minutes = binding.npMinutes.value
        val seconds = binding.npSeconds.value
        return (hours * 3600 + minutes * 60 + seconds) * 1000L
    }

    private fun updateCredentialButton() {
        when (selectedType?.type) {
            AuthenticatorType.PIN -> {
                binding.setCredentialButton.visibility = View.VISIBLE
                binding.setCredentialButton.setText(R.string.set_pin)
                binding.setCredentialButton.setIconResource(R.drawable.ic_pin)
            }
            AuthenticatorType.PATTERN -> {
                binding.setCredentialButton.visibility = View.VISIBLE
                binding.setCredentialButton.setText(R.string.set_pattern)
                binding.setCredentialButton.setIconResource(R.drawable.ic_pattern)
            }
            else -> {
                binding.setCredentialButton.visibility = View.GONE
            }
        }
    }

    private fun updateSaveButtonState() {
        binding.saveButton.isEnabled = when (selectedType?.type) {
            AuthenticatorType.PIN -> PinCache.hasPin(this)
            AuthenticatorType.PATTERN -> PatternCache.hasPattern(this)
            AuthenticatorType.BIOMETRIC -> true
            else -> false
        }
    }
}
