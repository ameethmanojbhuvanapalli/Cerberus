package com.example.cerberus.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.cerberus.R

class PermissionHelperActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_helper)

        val accessibilityPermissionButton: Button = findViewById(R.id.button_accessibility_permission)
        val overlayPermissionButton: Button = findViewById(R.id.button_overlay_permission)

        accessibilityPermissionButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        overlayPermissionButton.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun hasAccessibilityPermission(context: Context): Boolean {
        val expectedServiceId = "${context.packageName}/.service.AppLockService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val service = colonSplitter.next()
            if (service.equals(expectedServiceId, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
