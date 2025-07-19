package com.example.cerberus.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import com.example.cerberus.auth.PromptActivityManager
import com.example.cerberus.data.PasswordCache
import com.example.cerberus.databinding.ActivityPasswordPromptBinding
import com.example.cerberus.utils.HashUtil

class PasswordPromptActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPasswordPromptBinding
    private val TAG = "PasswordPromptActivity"
    private var packageNameToAuth: String? = null
    private var isRegisteredWithManager = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordPromptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        packageNameToAuth = intent.getStringExtra("packageName")
        
        // Register with PromptActivityManager to prevent multiple prompts
        if (packageNameToAuth != null && !PromptActivityManager.registerPrompt(packageNameToAuth!!, "password")) {
            Log.d(TAG, "Another prompt is already active for $packageNameToAuth, finishing")
            finish()
            return
        }
        isRegisteredWithManager = true

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.d(TAG, "Back button pressed - dismissing activity")
                    sendAuthDismissedBroadcast()
                    finish()
                }
            }
        )

        binding.confirmButton.setOnClickListener {
            val pwd = binding.passwordInput.text.toString()
            if (HashUtil.hash(pwd) == PasswordCache.getPasswordHash(this)) {
                sendBroadcast(Intent("com.example.cerberus.AUTH_SUCCESS"))
                finish()
            } else {
                sendBroadcast(Intent("com.example.cerberus.AUTH_FAILURE"))
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                binding.passwordInput.text?.clear()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Prompt activities should only be foreground or destroyed
        // If we're being paused, finish immediately to return to home screen
        Log.d(TAG, "Activity paused - finishing to return to home screen")
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister from PromptActivityManager
        if (isRegisteredWithManager && packageNameToAuth != null) {
            PromptActivityManager.unregisterPrompt(packageNameToAuth!!, "password")
        }
    }

    private fun sendAuthDismissedBroadcast() {
        val intent = Intent("com.example.cerberus.AUTH_DISMISSED")
        intent.putExtra("packageName", packageNameToAuth)
        sendBroadcast(intent)
        Log.d(TAG, "Sent AUTH_DISMISSED broadcast for $packageNameToAuth")
    }
}
