package com.example.cerberus.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cerberus.R
import com.example.cerberus.auth.AuthenticatorType
import com.example.cerberus.data.AuthenticatorTypeCache
import com.example.cerberus.model.AuthenticatorTypeItem
import com.example.cerberus.data.PinCache
import com.example.cerberus.databinding.ActivityAuthSettingsBinding

class AuthSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthSettingsBinding
    private lateinit var adapter: AuthTypeSelectAdapter
    private lateinit var authTypes: List<AuthenticatorTypeItem>
    private var selectedType: AuthenticatorTypeItem? = null
    private var isCredentialSet: Boolean = false

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

        // Get last selected or default to first
        val cachedType = AuthenticatorTypeCache.getAuthenticatorType(this)
        selectedType = authTypes.find { it.type == cachedType } ?: authTypes.first()

        adapter = AuthTypeSelectAdapter(this, authTypes, selectedType!!) { selected ->
            selectedType = selected
            isCredentialSet = false
            updateCredentialButton()
            updateSaveButtonState()
        }

        binding.authTypeListView.adapter = adapter

        binding.setCredentialButton.setOnClickListener {
            when (selectedType?.type) {
                AuthenticatorType.PIN -> {
                    val fragment = PinSetupFragment().apply {
                        onPinSet = {
                            isCredentialSet = true
                            updateSaveButtonState()
                            Toast.makeText(context, "PIN set", Toast.LENGTH_SHORT).show()
                        }
                    }
                    fragment.show(supportFragmentManager, "pin_setup")
                }
                AuthenticatorType.PATTERN -> {
                    isCredentialSet = true
                    Toast.makeText(this, "Set Pattern action", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }

        binding.saveButton.setOnClickListener {
            selectedType?.let {
                AuthenticatorTypeCache.setAuthenticatorType(this, it.type)
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
            AuthenticatorType.PATTERN -> isCredentialSet
            AuthenticatorType.BIOMETRIC -> true
            else -> false
        }
    }
}