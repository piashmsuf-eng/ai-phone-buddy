package com.myra.assistant.ui.main

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.jvm.JvmOverloads
import kotlin.math.abs

class WaveformView : View {
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : super(context, attrs, defStyleAttr)

    private val barCount = 20
    private val barHeights = FloatArray(barCount) { 5f }
    private val targetHeights = FloatArray(barCount) { 5f }
    private var amplitude = 0f
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF1744") }
    private var isAnimating = false

    private val animator: ValueAnimator

    init {
        var a: ValueAnimator? = null
        try {
            a = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 50; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.RESTART
                addUpdateListener {
                    for (i in 0 until barCount) {
                        barHeights[i] += (targetHeights[i] - barHeights[i]) * 0.3f
                    }
                    invalidate()
                }
            }
        } catch (_: Exception) {}
        animator = a ?: ValueAnimator.ofFloat(0f, 0f)
    }

    fun setAmplitude(rms: Float) {
        amplitude = rms.coerceIn(0f, 1f)
        for (i in 0 until barCount) {
            val factor = 1f - abs(i - barCount / 2f) / (barCount / 2f)
            targetHeights[i] = (5f + amplitude * factor * 30f).coerceAtLeast(3f)
        }
    }

    fun startAnimation() { if (!isAnimating) { isAnimating = true; try { animator.start() } catch (_: Exception) {} } }
    fun stopAnimation() { isAnimating = false; try { animator.cancel() } catch (_: Exception) {} }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val barWidth = width.toFloat() / barCount
        for (i in 0 until barCount) {
            val h = barHeights[i]
            barPaint.alpha = (150 + h * 3).toInt().coerceIn(0, 255)
            val left = i * barWidth + 2
            val right = left + barWidth - 4
            canvas.drawRoundRect(left, height / 2f - h / 2f, right, height / 2f + h / 2f, 2f, 2f, barPaint)
        }
    }
}
