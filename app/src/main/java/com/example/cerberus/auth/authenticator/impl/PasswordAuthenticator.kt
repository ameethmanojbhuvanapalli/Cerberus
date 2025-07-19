package com.example.cerberus.auth.authenticator.impl

import android.content.*
import androidx.core.content.ContextCompat
import com.example.cerberus.auth.authenticator.Authenticator
import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.ui.activity.PasswordPromptActivity

class PasswordAuthenticator : Authenticator {
    private val callbacks = mutableListOf<AuthenticationCallback>()
    private var lastPackageName: String? = null
    private var receiverRegistered = false
    private var lastContext: Context? = null
    private val TAG = "PasswordAuthenticator"

    override fun authenticate(context: Context, packageName: String) {
        lastPackageName = packageName
        lastContext = context.applicationContext
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction("com.example.cerberus.AUTH_SUCCESS")
                addAction("com.example.cerberus.AUTH_FAILURE")
                addAction("com.example.cerberus.AUTH_DISMISSED")
            }
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered = true
        }
        val intent = Intent(context, PasswordPromptActivity::class.java).apply {
            putExtra("packageName", packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        context.startActivity(intent)
    }

    override fun registerCallback(cb: AuthenticationCallback) { if (!callbacks.contains(cb)) callbacks.add(cb) }
    override fun unregisterCallback(cb: AuthenticationCallback) { callbacks.remove(cb) }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.example.cerberus.AUTH_SUCCESS" -> notifyAuthenticationSucceeded()
                "com.example.cerberus.AUTH_FAILURE" -> notifyAuthenticationFailed()
                "com.example.cerberus.AUTH_DISMISSED" -> notifyAuthenticationFailed()
            }
            try { lastContext?.unregisterReceiver(this) } catch (_: Exception) {}
            receiverRegistered = false
        }
    }

    private fun notifyAuthenticationSucceeded() {
        lastPackageName?.let { pkg -> callbacks.forEach { it.onAuthenticationSucceeded(pkg) } }
    }
    private fun notifyAuthenticationFailed() {
        lastPackageName?.let { pkg -> callbacks.forEach { it.onAuthenticationFailed(pkg) } }
    }
}
