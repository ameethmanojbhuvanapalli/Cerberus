package com.example.cerberus.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cerberus.R
import com.example.cerberus.utils.PermissionManager
import com.google.android.material.button.MaterialButton

class PermissionHelperFragment(
    private val onPermissionsGranted: () -> Unit
) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_permission_helper, container, false)
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
        if (PermissionManager.hasAccessibilityPermission(requireContext()) &&
            PermissionManager.hasOverlayPermission(requireContext())
        ) {
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
            onPermissionsGranted()
        }
    }
}