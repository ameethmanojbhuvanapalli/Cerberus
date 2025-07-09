package com.example.cerberus.applock

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.example.cerberus.R
import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.auth.AuthenticationManager
import com.example.cerberus.auth.AuthChannel
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
            if (PermissionsUtil.hasAllRequiredPermissions(this)) {
                setProtectionEnabled(true)
                updateTileState()
                sendProtectionStateChangedBroadcast()
                Toast.makeText(this, "Protection Enabled", Toast.LENGTH_SHORT).show()
            } else {
                // Launch the main activity of your app (where you handle permissions)
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                if (launchIntent != null) {
                    if (android.os.Build.VERSION.SDK_INT >= 34) {
                        val pendingIntent = android.app.PendingIntent.getActivity(
                            this, 0, launchIntent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        startActivityAndCollapse(pendingIntent)
                    } else {
                        startActivity(launchIntent)
                    }
                }
            }
        } else {
            val authService = AuthenticationManager.getInstance(applicationContext).getAuthService()

            val callback = object : AuthenticationCallback {
                override fun onAuthenticationSucceeded(packageName: String) {
                    setProtectionEnabled(false)
                    Toast.makeText(applicationContext, "Protection Disabled", Toast.LENGTH_SHORT).show()
                    updateTileState()
                    sendProtectionStateChangedBroadcast()
                }

                override fun onAuthenticationFailed(packageName: String) {
                    Toast.makeText(applicationContext, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }

            val requested = authService.requestAuthenticationIfNeeded(
                AuthChannel.TILE,
                applicationContext.packageName,
                callback
            )

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
}