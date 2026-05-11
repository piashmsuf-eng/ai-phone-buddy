package com.myra.assistant.ui.main

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.abs

class WaveformView : View {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val barCount = 20
    private val barHeights = FloatArray(barCount) { 5f }
    private val targetHeights = FloatArray(barCount) { 5f }
    private var amplitude = 0f
    private var barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF1744") }
    private var isAnimating = false

    private var animator: ValueAnimator? = null

    init {
        try {
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 50; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.RESTART
                addUpdateListener {
                    for (i in 0 until barCount) {
                        barHeights[i] += (targetHeights[i] - barHeights[i]) * 0.3f
                    }
                    try { invalidate() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e("MYRA_WAV", "init animator failed", e)
        }
    }

    fun setAmplitude(rms: Float) {
        amplitude = rms.coerceIn(0f, 1f)
        for (i in 0 until barCount) {
            val factor = 1f - abs(i - barCount / 2f) / (barCount / 2f)
            targetHeights[i] = (5f + amplitude * factor * 30f).coerceAtLeast(3f)
        }
    }

    fun startAnimation() { if (!isAnimating) { isAnimating = true; try { animator?.start() } catch (_: Exception) {} } }
    fun stopAnimation() { isAnimating = false; try { animator?.cancel() } catch (_: Exception) {} }

    override fun onDraw(canvas: Canvas) {
        try {
            drawBars(canvas)
        } catch (e: Exception) {
            Log.e("MYRA_WAV", "onDraw crash", e)
        }
    }

    private fun drawBars(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val barWidth = w / barCount
        for (i in 0 until barCount) {
            val barH = barHeights[i]
            barPaint.alpha = (150 + barH * 3).toInt().coerceIn(0, 255)
            val left = i * barWidth + 2
            val right = left + barWidth - 4
            canvas.drawRoundRect(left, h / 2f - barH / 2f, right, h / 2f + barH / 2f, 2f, 2f, barPaint)
        }
    }
}
