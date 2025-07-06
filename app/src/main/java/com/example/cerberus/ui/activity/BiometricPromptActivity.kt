package com.example.cerberus.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.OnBackPressedCallback

class BiometricPromptActivity : FragmentActivity() {
    private val TAG = "BiometricPromptActivity"
    private var packageNameToAuth: String? = null
    private var promptShowing = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        super.onCreate(savedInstanceState)
        Log.d(TAG, "BiometricPromptActivity created")
        packageNameToAuth = intent?.getStringExtra("packageName") ?: applicationContext.packageName

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.d(TAG, "Back button pressed - re-triggering prompt")
                    retriggerPrompt()
                }
            }
        )
        showBiometricPrompt()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Always send a "prompt finished" broadcast
        sendPromptFinishedBroadcast()
    }

    private fun sendPromptFinishedBroadcast() {
        val intent = Intent("com.example.cerberus.AUTH_PROMPT_FINISHED")
        intent.putExtra("packageName", packageNameToAuth)
        sendBroadcast(intent)
    }

    private fun retriggerPrompt() {
        // Prevent multiple triggers
        if (!promptShowing) {
            handler.post { showBiometricPrompt() }
        }
    }

    private fun showBiometricPrompt() {
        if (isFinishing || promptShowing) return

        promptShowing = true
        Log.d(TAG, "Showing biometric prompt")
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    Log.d(TAG, "Authentication succeeded, sending broadcast")
                    val intent = Intent("com.example.cerberus.AUTH_SUCCESS")
                    sendBroadcast(intent)
                    finish()
                }

                override fun onAuthenticationFailed() {
                    Log.d(TAG, "Authentication failed, re-triggering prompt")
                    promptShowing = false
                    retriggerPrompt()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Log.d(TAG, "Authentication error: $errorCode - $errString")
                    promptShowing = false
                    retriggerPrompt()
                }
            }
        )

        try {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("App Locked")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            biometricPrompt.authenticate(promptInfo)
            Log.d(TAG, "Biometric prompt shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing biometric prompt", e)
            promptShowing = false
            retriggerPrompt()
        }
    }
}