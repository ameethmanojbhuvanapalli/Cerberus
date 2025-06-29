package com.example.cerberus.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment

class IdleTimeoutPickerFragment(
    private val initialTimeoutMs: Long,
    private val onTimeoutSet: (Long) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val picker = NumberPicker(requireContext()).apply {
            minValue = 1
            maxValue = 60
            value = (initialTimeoutMs / 1000).toInt().coerceIn(minValue, maxValue)
            wrapSelectorWheel = false
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Set Idle Timeout (seconds)")
            .setView(picker)
            .setPositiveButton("Set") { _, _ ->
                val selectedMs = picker.value * 1000L
                onTimeoutSet(selectedMs)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}
