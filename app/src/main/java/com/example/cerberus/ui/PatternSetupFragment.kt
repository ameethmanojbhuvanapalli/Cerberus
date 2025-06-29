package com.example.cerberus.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.cerberus.data.PatternCache
import com.example.cerberus.databinding.FragmentPatternSetupBinding
import com.itsxtt.patternlock.PatternLockView

class PatternSetupFragment : DialogFragment() {
    private var _bind: FragmentPatternSetupBinding? = null
    private val binding get() = _bind!!
    var onPatternSet: (() -> Unit)? = null
    private var first: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _bind = FragmentPatternSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.patternLockView.setOnPatternListener(object : PatternLockView.OnPatternListener {
            override fun onComplete(ids: ArrayList<Int>): Boolean {
                val str = ids.joinToString("-")
                return if (first == null) {
                    first = str
                    Toast.makeText(context, "Confirm pattern", Toast.LENGTH_SHORT).show()
                    false
                } else if (first == str) {
                    PatternCache.setPattern(requireContext(), str)
                    onPatternSet?.invoke()
                    dismiss()
                    true
                } else {
                    Toast.makeText(context, "Patterns do not match", Toast.LENGTH_SHORT).show()
                    first = null
                    false
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _bind = null
    }
}
