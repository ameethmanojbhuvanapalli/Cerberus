package com.example.cerberus.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.cerberus.data.PinCache
import com.example.cerberus.databinding.FragmentPinSetupBinding
import com.example.cerberus.utils.HashUtils

class PinSetupFragment : DialogFragment() {
    private var _binding: FragmentPinSetupBinding? = null
    private val binding get() = _binding!!
    var onPinSet: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPinSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.confirmButton.setOnClickListener {
            val pin = binding.pinInput.text.toString()
            val pinConfirm = binding.pinConfirmInput.text.toString()
            if (pin.length != 4) {
                Toast.makeText(context, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pin != pinConfirm) {
                Toast.makeText(context, "PINs do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val hash = HashUtils.hash(pin)
            PinCache.setPin(requireContext(), hash)
            onPinSet?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}