package com.example.cerberus.applock

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.cerberus.R
import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.auth.AuthenticationManager
import com.example.cerberus.data.ProtectionCache
import com.example.cerberus.utils.PermissionsUtil

class AppLockTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        val enabled = isProtectionEnabled()

        if (!enabled) {
            setProtectionEnabled(true)
            updateTileState()
            sendProtectionStateChangedBroadcast()

            if (PermissionsUtil.hasAccessibilityPermission(this)) {
                sendBroadcast(Intent("com.example.cerberus.START_LOCK_SERVICE"))
                Toast.makeText(this, "Protection Enabled", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    launchSettingsWithPendingIntent(intent)
                } else {
                    launchSettingsWithIntent(intent)
                }

                Toast.makeText(this, "Grant Accessibility Permission", Toast.LENGTH_SHORT).show()
            }
        } else {
            val authService = AuthenticationManager.getInstance(applicationContext).getAuthService()

            val callback = object : AuthenticationCallback {
                override fun onAuthenticationSucceeded(packageName: String) {
                    setProtectionEnabled(false)
                    sendBroadcast(Intent("com.example.cerberus.STOP_LOCK_SERVICE"))
                    Toast.makeText(applicationContext, "Protection Disabled", Toast.LENGTH_SHORT).show()
                    updateTileState()
                    sendProtectionStateChangedBroadcast()
                    authService?.unregisterCallback(this)
                }

                override fun onAuthenticationFailed(packageName: String) {
                    Toast.makeText(applicationContext, "Authentication Failed", Toast.LENGTH_SHORT).show()
                    authService?.unregisterCallback(this)
                }
            }

            authService?.registerCallback(callback)

            val requested = authService?.requestAuthenticationIfNeeded(applicationContext.packageName) ?: false

            if (!requested) {
                callback.onAuthenticationSucceeded(applicationContext.packageName)
            }
        }
    }

    private fun updateTileState() {
        qsTile?.apply {
            state = if (isProtectionEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = if (isProtectionEnabled()) "Protection ON" else "Protection OFF"
            icon = Icon.createWithResource(this@AppLockTileService, R.drawable.cerberus_1)
            updateTile()
        }
    }

    private fun isProtectionEnabled(): Boolean {
        return ProtectionCache.isProtectionEnabled(this)
    }

    private fun setProtectionEnabled(enabled: Boolean) {
        ProtectionCache.setProtectionEnabled(this, enabled)
    }

    private fun sendProtectionStateChangedBroadcast() {
        sendBroadcast(Intent("com.example.cerberus.PROTECTION_STATE_CHANGED"))
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun launchSettingsWithPendingIntent(intent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        startActivityAndCollapse(pendingIntent)
    }

    @Suppress("DEPRECATION")
    private fun launchSettingsWithIntent(intent: Intent) {
        startActivity(intent)
    }
}
