package com.example.cerberus.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cerberus.data.PinCache
import com.example.cerberus.databinding.ActivityPinPromptBinding
import com.example.cerberus.utils.HashUtil

class PinPromptActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPinPromptBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinPromptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val packageName = intent.getStringExtra("packageName")

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
}