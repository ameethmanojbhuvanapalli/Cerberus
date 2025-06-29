package com.example.cerberus.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cerberus.data.PatternCache
import com.example.cerberus.databinding.ActivityPatternPromptBinding
import com.itsxtt.patternlock.PatternLockView

class PatternPromptActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPatternPromptBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatternPromptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val stored = PatternCache.getPatternHash(this) ?: return finish()

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
}
