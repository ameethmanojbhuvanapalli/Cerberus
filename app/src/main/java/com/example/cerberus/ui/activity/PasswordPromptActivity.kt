package com.example.cerberus.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cerberus.data.PasswordCache
import com.example.cerberus.databinding.ActivityPasswordPromptBinding
import com.example.cerberus.utils.HashUtil

class PasswordPromptActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPasswordPromptBinding
    private var packageNameToAuth: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordPromptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        packageNameToAuth = intent.getStringExtra("packageName")

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

    override fun onDestroy() {
        super.onDestroy()
        // Send prompt finished broadcast
        val intent = Intent("com.example.cerberus.AUTH_PROMPT_FINISHED")
        intent.putExtra("packageName", packageNameToAuth)
        sendBroadcast(intent)
    }
}
