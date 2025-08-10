package com.example.cerberus.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.cerberus.R

class FastScrollerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val letters = ('A'..'Z').toList().map { it.toString() }
    private var selectedLetter: String? = null
    private var onLetterSelected: ((String) -> Unit)? = null
    private var isPressed = false

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.fast_scroller_text_size)
    }

    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.fast_scroller_background)
    }

    private val selectedBackgroundPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.red_dark)
    }

    private val backgroundRect = RectF()
    private val letterHeight: Float
        get() = (height - paddingTop - paddingBottom) / letters.size.toFloat()

    fun setOnLetterSelectedListener(listener: (String) -> Unit) {
        onLetterSelected = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resources.getDimensionPixelSize(R.dimen.fast_scroller_width)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            heightMeasureSpec
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isPressed) {
            // Draw background when pressed
            backgroundRect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(backgroundRect, 12f, 12f, backgroundPaint)
        }

        // Draw letters
        letters.forEachIndexed { index, letter ->
            val x = width / 2f
            val y = paddingTop + (index + 0.5f) * letterHeight + textPaint.textSize / 3f

            // Draw selected letter background
            if (letter == selectedLetter && isPressed) {
                val letterTop = paddingTop + index * letterHeight
                val letterBottom = letterTop + letterHeight
                val selectionRect = RectF(
                    width * 0.1f,
                    letterTop + letterHeight * 0.1f,
                    width * 0.9f,
                    letterBottom - letterHeight * 0.1f
                )
                canvas.drawRoundRect(selectionRect, 8f, 8f, selectedBackgroundPaint)
                textPaint.color = ContextCompat.getColor(context, android.R.color.white)
            } else {
                textPaint.color = ContextCompat.getColor(context, R.color.gray)
            }

            canvas.drawText(letter, x, y, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                handleTouch(event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isPressed) {
                    handleTouch(event.y)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                selectedLetter = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTouch(y: Float) {
        val adjustedY = y - paddingTop
        val index = (adjustedY / letterHeight).toInt().coerceIn(0, letters.size - 1)
        val letter = letters[index]
        
        if (letter != selectedLetter) {
            selectedLetter = letter
            onLetterSelected?.invoke(letter)
            invalidate()
        }
    }
}