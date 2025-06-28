package com.example.cerberus.ui

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

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        super.onCreate(savedInstanceState)
        Log.d(TAG, "BiometricPromptActivity created")

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.d(TAG, "Back button pressed - ignoring")
                    // Do nothing to block back press
                }
            }
        )
        showBiometricPrompt()
    }

    private fun showBiometricPrompt() {
        if (isFinishing) return

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
                    Log.d(TAG, "Authentication failed, sending broadcast and retrying")
                    val intent = Intent("com.example.cerberus.AUTH_FAILURE")
                    sendBroadcast(intent)
                    showBiometricPrompt() // Try again
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Log.d(TAG, "Authentication error: $errorCode - $errString")

                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> {
                            Log.d(TAG, "User canceled authentication, showing prompt again")
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (!isFinishing) {
                                    showBiometricPrompt()
                                }
                            }, 100)
                        }
                        else -> {
                            val intent = Intent("com.example.cerberus.AUTH_FAILURE")
                            sendBroadcast(intent)
                            finish()
                        }
                    }
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
            val intent = Intent("com.example.cerberus.AUTH_FAILURE")
            sendBroadcast(intent)
            finish()
        }
    }
}