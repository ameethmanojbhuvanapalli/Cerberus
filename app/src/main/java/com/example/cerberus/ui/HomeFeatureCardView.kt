package com.example.cerberus.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import com.example.cerberus.R
import com.example.cerberus.databinding.ViewHomeFeatureCardBinding

class HomeFeatureCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val binding = ViewHomeFeatureCardBinding.inflate(LayoutInflater.from(context), this, true)

    fun setTitle(title: String) {
        binding.featureTitle.text = title
    }

    fun setDescription(desc: String) {
        binding.featureDesc.text = desc
    }

    fun setPillText(pill: String) {
        binding.featurePill.text = pill
    }

    fun setIcon(resId: Int) {
        binding.featureIcon.setImageResource(resId)
    }

    fun setIconContentDescription(desc: String) {
        binding.featureIcon.contentDescription = desc
    }

    override fun setOnClickListener(l: OnClickListener?) {
        binding.featureCard.setOnClickListener(l)
    }
}