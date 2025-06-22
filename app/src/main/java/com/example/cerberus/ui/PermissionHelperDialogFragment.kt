package com.example.cerberus.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.cerberus.R
import com.example.cerberus.utils.PermissionManager
import com.google.android.material.button.MaterialButton

class PermissionHelperDialogFragment : DialogFragment() {

    private var permissionGrantedCallback: (() -> Unit)? = null

    fun setPermissionGrantedCallback(callback: () -> Unit) {
        permissionGrantedCallback = callback
    }

    // Call this when permissions are granted
    fun notifyPermissionsGranted() {
        permissionGrantedCallback?.invoke()
        dismiss()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_permission_helper, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<MaterialButton>(R.id.button_accessibility_permission).setOnClickListener {
            if (!PermissionManager.hasAccessibilityPermission(requireContext())) {
                PermissionManager.requestAccessibilityPermission(requireActivity())
            } else {
                Toast.makeText(context, "Accessibility permission already granted", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<MaterialButton>(R.id.button_overlay_permission).setOnClickListener {
            if (!PermissionManager.hasOverlayPermission(requireContext())) {
                PermissionManager.requestOverlayPermission(requireActivity())
            } else {
                Toast.makeText(context, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Auto-dismiss if permissions granted
        if (PermissionManager.hasAccessibilityPermission(requireContext()) &&
            PermissionManager.hasOverlayPermission(requireContext())
        ) {
            notifyPermissionsGranted()
        }
    }

    override fun onStart() {
        super.onStart()
        // Make dialog fullscreen
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.bg_gradient_black) // or your gradient drawable
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        super.onCreateDialog(savedInstanceState).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
}