package com.example.cerberus.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.cerberus.R
import com.example.cerberus.utils.PermissionManager

class PermissionHelperActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_helper)

        val accessibilityButton: Button = findViewById(R.id.button_accessibility_permission)
        val overlayButton: Button = findViewById(R.id.button_overlay_permission)

        accessibilityButton.setOnClickListener {
            if(!PermissionManager.hasAccessibilityPermission(this)){
                PermissionManager.requestAccessibilityPermission(this)
            } else {
                Toast.makeText(this, "Accessibility permission already granted", Toast.LENGTH_SHORT).show()
            }
        }

        overlayButton.setOnClickListener {
            if (!PermissionManager.hasOverlayPermission(this)) {
                PermissionManager.requestOverlayPermission(this)
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
