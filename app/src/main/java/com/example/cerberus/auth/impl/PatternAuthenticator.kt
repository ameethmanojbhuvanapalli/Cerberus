package com.example.cerberus.auth.impl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.example.cerberus.auth.Authenticator
import com.example.cerberus.auth.AuthenticationCallback
import com.example.cerberus.ui.PatternPromptActivity


class PatternAuthenticator : Authenticator {
    private val callbacks = mutableListOf<AuthenticationCallback>()
    private var lastPkg: String? = null
    private var recRegistered = false
    private var lastCtx: Context? = null

    override fun authenticate(context: Context, packageName: String) {
        lastPkg = packageName
        lastCtx = context.applicationContext
        if (!recRegistered) {
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
            recRegistered = true
        }
        val intent = Intent(context, PatternPromptActivity::class.java).apply {
            putExtra("packageName", packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        context.startActivity(intent)
    }

    override fun registerCallback(callback: AuthenticationCallback) {
        if (!callbacks.contains(callback)) callbacks.add(callback)
    }

    override fun unregisterCallback(callback: AuthenticationCallback) {
        callbacks.remove(callback)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when(intent.action) {
                "com.example.cerberus.AUTH_SUCCESS" ->
                    lastPkg?.let { pkg -> callbacks.forEach { it.onAuthenticationSucceeded(pkg) } }
                "com.example.cerberus.AUTH_FAILURE" ->
                    lastPkg?.let { pkg -> callbacks.forEach { it.onAuthenticationFailed(pkg) } }
            }
            try { lastCtx?.unregisterReceiver(this) } catch(_: Exception) {}
            recRegistered = false
        }
    }
}
