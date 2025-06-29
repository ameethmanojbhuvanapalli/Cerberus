package com.example.cerberus.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.cerberus.R
import com.example.cerberus.data.IdleTimeoutCache

class IdleTimeoutPickerFragment(
    private val onTimeoutSet: (Long) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.fragment_idle_timeout_picker, null)

        val hoursInput = view.findViewById<EditText>(R.id.input_hours)
        val minutesInput = view.findViewById<EditText>(R.id.input_minutes)

        val initialTimeoutMs = IdleTimeoutCache.getIdleTimeout(requireContext())
        val totalMinutes = (initialTimeoutMs / 1000 / 60).toInt()
        val initialHours = totalMinutes / 60
        val initialMinutes = totalMinutes % 60

        hoursInput.setText(initialHours.toString())
        minutesInput.setText(initialMinutes.toString())

        return AlertDialog.Builder(requireContext(), R.style.Theme_Cerberus)
            .setView(view)
            .setPositiveButton("Set", null)
            .setNegativeButton("Cancel", null)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.SecureBlockerTheme)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? AlertDialog ?: return
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        val hoursInput = dialog.findViewById<EditText>(R.id.input_hours)
        val minutesInput = dialog.findViewById<EditText>(R.id.input_minutes)

        positiveButton.setOnClickListener {
            val hours = hoursInput?.text?.toString()?.toIntOrNull()
            val minutes = minutesInput?.text?.toString()?.toIntOrNull()

            if (hours == null || minutes == null || hours !in 0..23 || minutes !in 5..59) {
                Toast.makeText(requireContext(), "Enter valid hours (0–23) and minutes (5–59)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val totalMinutes = hours * 60 + minutes
            val timeoutMs = totalMinutes * 60 * 1000L
            onTimeoutSet(timeoutMs)
            dismiss()
        }
    }

}
