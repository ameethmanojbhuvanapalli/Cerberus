package com.example.cerberus.auth.impl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.auth.Authenticator
import com.example.cerberus.ui.BiometricPromptActivity

class BiometricAuthenticator : Authenticator {
    private val callbacks = mutableListOf<AuthenticationCallback>()
    private var lastPackageName: String? = null
    private var receiver: BroadcastReceiver? = null
    private val TAG = "BiometricAuthenticator"

    override fun authenticate(context: Context, packageName: String) {
        Log.d(TAG, "Authentication requested for: $packageName")
        lastPackageName = packageName

        // Unregister any existing receiver
        unregisterReceiver(context)

        // Register a new receiver for auth results
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                Log.d(TAG, "Broadcast received: ${intent?.action}")
                when (intent?.action) {
                    "com.example.cerberus.AUTH_SUCCESS" -> {
                        Log.d(TAG, "Auth success broadcast received")
                        notifyAuthenticationSucceeded()
                    }
                    "com.example.cerberus.AUTH_FAILURE" -> {
                        Log.d(TAG, "Auth failure broadcast received")
                        notifyAuthenticationFailed()
                    }
                }
            }
        }

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

        // Launch biometric authentication
        Log.d(TAG, "Launching BiometricPromptActivity")
        val intent = Intent(context, BiometricPromptActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        context.startActivity(intent)
    }

    override fun registerCallback(callback: AuthenticationCallback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback)
            Log.d(TAG, "Callback registered, total callbacks: ${callbacks.size}")
        }
    }

    override fun unregisterCallback(callback: AuthenticationCallback) {
        callbacks.remove(callback)
        Log.d(TAG, "Callback unregistered, remaining callbacks: ${callbacks.size}")
    }

    private fun notifyAuthenticationSucceeded() {
        lastPackageName?.let { packageName ->
            Log.d(TAG, "Notifying ${callbacks.size} callbacks of success for: $packageName")
            callbacks.forEach { it.onAuthenticationSucceeded(packageName) }
        }
    }

    private fun notifyAuthenticationFailed() {
        lastPackageName?.let { packageName ->
            Log.d(TAG, "Notifying ${callbacks.size} callbacks of failure for: $packageName")
            callbacks.forEach { it.onAuthenticationFailed(packageName) }
        }
    }

    private fun unregisterReceiver(context: Context) {
        receiver?.let {
            try {
                context.unregisterReceiver(it)
                Log.d(TAG, "Successfully unregistered receiver")
            } catch (e: IllegalArgumentException) {
                Log.d(TAG, "Receiver not registered, skipping unregister")
                // Receiver not registered, ignore
            }
        }
        receiver = null
    }
}