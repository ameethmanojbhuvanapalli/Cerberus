package com.example.cerberus.ui.card

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.cerberus.R
import com.example.cerberus.data.ProtectionCache
import com.example.cerberus.databinding.ProtectionToggleCardBinding

class ProtectionCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ProtectionToggleCardBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = ProtectionToggleCardBinding.inflate(inflater, this)
    }

    fun updateProtectionState() {
        val isEnabled = ProtectionCache.isProtectionEnabled(context)
        val newDrawableRes = if (isEnabled) R.drawable.cerberus_1 else R.drawable.cerberus_2
        val newTextColor = if (isEnabled) R.color.red_dark else R.color.gray
        val fadeDuration = 300L

        binding.imgProtectionStatus.animate()
            .alpha(0f)
            .setDuration(fadeDuration)
            .withEndAction {
                binding.imgProtectionStatus.setImageResource(newDrawableRes)
                binding.imgProtectionStatus.animate()
                    .alpha(1f)
                    .setDuration(fadeDuration)
                    .start()
            }
            .start()

        binding.txtProtectionStatus.text = if (isEnabled) {
            context.getString(R.string.protection_on)
        } else {
            context.getString(R.string.protection_off)
        }

        binding.txtProtectionStatus.setTextColor(ContextCompat.getColor(context, newTextColor))
    }



    fun toggleProtectionState() {
        val newState = !ProtectionCache.isProtectionEnabled(context)
        ProtectionCache.setProtectionEnabled(context, newState)
        updateProtectionState()
    }
}
