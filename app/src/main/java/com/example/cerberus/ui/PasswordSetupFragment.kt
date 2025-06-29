package com.example.cerberus.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.cerberus.data.PasswordCache
import com.example.cerberus.databinding.FragmentPasswordSetupBinding
import com.example.cerberus.utils.HashUtil

class PasswordSetupFragment : DialogFragment() {
    private var _binding: FragmentPasswordSetupBinding? = null
    private val binding get() = _binding!!
    var onPasswordSet: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPasswordSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.confirmButton.setOnClickListener {
            val pwd = binding.passwordInput.text.toString()
            val pwdConfirm = binding.passwordConfirmInput.text.toString()
            if (pwd.length < 10 || !pwd.any(Char::isUpperCase) || !pwd.any(Char::isLowerCase)
                || !pwd.any(Char::isDigit) || !pwd.any { !it.isLetterOrDigit() }) {
                Toast.makeText(context, "Password ≥10 chars with upper,lower,digit,special", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (pwd != pwdConfirm) {
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            PasswordCache.setPassword(requireContext(), HashUtil.hash(pwd))
            onPasswordSet?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

