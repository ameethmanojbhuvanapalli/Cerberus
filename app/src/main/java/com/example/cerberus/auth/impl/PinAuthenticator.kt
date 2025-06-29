package com.example.cerberus.auth.impl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.cerberus.auth.Authenticator
import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.ui.PinPromptActivity

class PinAuthenticator : Authenticator {
    private val callbacks = mutableListOf<AuthenticationCallback>()
    private var lastPackageName: String? = null
    private var receiverRegistered = false
    private var lastContext: Context? = null
    private val TAG = "PinAuthenticator"

    override fun authenticate(context: Context, packageName: String) {
        lastPackageName = packageName
        lastContext = context.applicationContext

        // Register broadcast receiver for result
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction("com.example.cerberus.AUTH_SUCCESS")
                addAction("com.example.cerberus.AUTH_FAILURE")
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }

        val intent = Intent(context, PinPromptActivity::class.java)
        intent.putExtra("packageName", packageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        context.startActivity(intent)
    }

    override fun registerCallback(callback: AuthenticationCallback) {
        if (!callbacks.contains(callback)) callbacks.add(callback)
    }

    override fun unregisterCallback(callback: AuthenticationCallback) {
        callbacks.remove(callback)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d(TAG, "Received broadcast: $action")
            when (action) {
                "com.example.cerberus.AUTH_SUCCESS" -> notifyAuthenticationSucceeded()
                "com.example.cerberus.AUTH_FAILURE" -> notifyAuthenticationFailed()
            }
            // Unregister after handling
            try {
                lastContext?.unregisterReceiver(this)
            } catch (_: Exception) {}
            receiverRegistered = false
        }
    }

    fun notifyAuthenticationSucceeded() {
        lastPackageName?.let { pkg -> callbacks.forEach { it.onAuthenticationSucceeded(pkg) } }
    }

    fun notifyAuthenticationFailed() {
        lastPackageName?.let { pkg -> callbacks.forEach { it.onAuthenticationFailed(pkg) } }
    }
}