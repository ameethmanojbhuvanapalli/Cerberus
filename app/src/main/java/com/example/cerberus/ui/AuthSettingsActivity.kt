package com.example.cerberus.ui

import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.cerberus.R
import com.example.cerberus.auth.AuthenticatorType
import com.example.cerberus.model.AuthenticatorTypeItem
import com.example.cerberus.data.AuthenticatorTypeCache

class AuthSettingsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: AuthTypeSelectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth_settings)

        listView = findViewById(R.id.auth_type_list_view)

        val items = listOf(
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

        val currentType = AuthenticatorTypeCache.getAuthenticatorType(this)
        val selectedItem = items.first { it.type == currentType }

        adapter = AuthTypeSelectAdapter(this, items, selectedItem) { selected ->
            AuthenticatorTypeCache.setAuthenticatorType(this, selected.type)
        }

        listView.adapter = adapter
    }
}