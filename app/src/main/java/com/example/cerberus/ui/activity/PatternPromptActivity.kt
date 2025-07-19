package com.example.cerberus.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import com.example.cerberus.auth.PromptActivityManager
import com.example.cerberus.data.PatternCache
import com.example.cerberus.databinding.ActivityPatternPromptBinding
import com.itsxtt.patternlock.PatternLockView

class PatternPromptActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPatternPromptBinding
    private val TAG = "PatternPromptActivity"
    private var packageNameToAuth: String? = null
    private var isRegisteredWithManager = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatternPromptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        packageNameToAuth = intent.getStringExtra("packageName")
        
        // Register with PromptActivityManager to prevent multiple prompts
        if (packageNameToAuth != null && !PromptActivityManager.registerPrompt(packageNameToAuth!!, "pattern")) {
            Log.d(TAG, "Another prompt is already active for $packageNameToAuth, finishing")
            finish()
            return
        }
        isRegisteredWithManager = true

        val stored = PatternCache.getPatternHash(this) ?: return finish()

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.d(TAG, "Back button pressed - dismissing activity")
                    sendAuthDismissedBroadcast()
                    finish()
                }
            }
        )

        binding.patternLockView.setOnPatternListener(object : PatternLockView.OnPatternListener {
            override fun onComplete(ids: ArrayList<Int>): Boolean {
                val entered = ids.joinToString("-")
                return if (entered == stored) {
                    sendBroadcast(Intent("com.example.cerberus.AUTH_SUCCESS"))
                    finish()
                    true
                } else {
                    sendBroadcast(Intent("com.example.cerberus.AUTH_FAILURE"))
                    Toast.makeText(this@PatternPromptActivity, "Incorrect pattern", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister from PromptActivityManager
        if (isRegisteredWithManager && packageNameToAuth != null) {
            PromptActivityManager.unregisterPrompt(packageNameToAuth!!, "pattern")
        }
    }

    private fun sendAuthDismissedBroadcast() {
        val intent = Intent("com.example.cerberus.AUTH_DISMISSED")
        intent.putExtra("packageName", packageNameToAuth)
        sendBroadcast(intent)
        Log.d(TAG, "Sent AUTH_DISMISSED broadcast for $packageNameToAuth")
    }
}
