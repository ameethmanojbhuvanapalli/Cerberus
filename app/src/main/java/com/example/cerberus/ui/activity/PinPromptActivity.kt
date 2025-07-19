package com.example.cerberus.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import com.example.cerberus.auth.PromptActivityManager
import com.example.cerberus.data.PinCache
import com.example.cerberus.databinding.ActivityPinPromptBinding
import com.example.cerberus.utils.HashUtil

class PinPromptActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPinPromptBinding
    private val TAG = "PinPromptActivity"
    private var packageNameToAuth: String? = null
    private var isRegisteredWithManager = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinPromptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        packageNameToAuth = intent.getStringExtra("packageName")
        
        // Register with PromptActivityManager to prevent multiple prompts
        if (packageNameToAuth != null && !PromptActivityManager.registerPrompt(packageNameToAuth!!, "pin")) {
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
            val pin = binding.pinInput.text.toString()
            val hash = HashUtil.hash(pin)
            if (PinCache.getPinHash(this) == hash) {
                sendBroadcast(Intent("com.example.cerberus.AUTH_SUCCESS"))
                finish()
            } else {
                sendBroadcast(Intent("com.example.cerberus.AUTH_FAILURE"))
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                binding.pinInput.text?.clear()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister from PromptActivityManager
        if (isRegisteredWithManager && packageNameToAuth != null) {
            PromptActivityManager.unregisterPrompt(packageNameToAuth!!, "pin")
        }
    }

    private fun sendAuthDismissedBroadcast() {
        val intent = Intent("com.example.cerberus.AUTH_DISMISSED")
        intent.putExtra("packageName", packageNameToAuth)
        sendBroadcast(intent)
        Log.d(TAG, "Sent AUTH_DISMISSED broadcast for $packageNameToAuth")
    }
}