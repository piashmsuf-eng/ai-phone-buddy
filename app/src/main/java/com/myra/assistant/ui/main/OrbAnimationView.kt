package com.myra.assistant.ui.main

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState { IDLE, LISTENING, SPEAKING, THINKING }

class OrbAnimationView(context: Context) : View(context) {
    var state = OrbState.IDLE
        set(value) { field = value; updateColors(); invalidate() }
    var amplitude = 0f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
    private var rotationAngle = 0f
    private var pulseScale = 1f
    private var waveOffset = 0f
    private var coreColor = 0xFFB71C1C.toInt()
    private var accentColor = 0xFFFF1744.toInt()

    private val rotateAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 3000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
        addUpdateListener { rotationAngle = it.animatedValue as Float; invalidate() }
    }
    private val pulseAnimator = ValueAnimator.ofFloat(1f, 1.15f).apply {
        duration = 1500; repeatMode = ValueAnimator.REVERSE; repeatCount = ValueAnimator.INFINITE
        addUpdateListener { pulseScale = it.animatedValue as Float; invalidate() }
    }
    private val waveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1200; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
        addUpdateListener { waveOffset = it.animatedValue as Float; invalidate() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        rotateAnimator.start()
        pulseAnimator.start()
        waveAnimator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        rotateAnimator.cancel()
        pulseAnimator.cancel()
        waveAnimator.cancel()
    }

    private fun updateColors() {
        when (state) {
            OrbState.IDLE -> { coreColor = 0xFFB71C1C.toInt(); accentColor = 0xFFFF1744.toInt() }
            OrbState.LISTENING -> { coreColor = 0xFFFF1744.toInt(); accentColor = 0xFFD500F9.toInt() }
            OrbState.SPEAKING -> { coreColor = 0xFFE040FB.toInt(); accentColor = 0xFFFF1744.toInt() }
            OrbState.THINKING -> { coreColor = 0xFF40C4FF.toInt(); accentColor = 0xFF00B0FF.toInt() }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f; val cy = height / 2f
        val radius = (minOf(width, height) / 2.8f) * pulseScale

        corePaint.shader = RadialGradient(cx - radius * 0.3f, cy - radius * 0.3f, radius * 1.6f,
            intArrayOf(Color.WHITE, accentColor, coreColor), floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius, corePaint)

        ringPaint.color = accentColor
        for (i in 0..2) {
            val r = radius * (1.4f + i * 0.25f)
            ringPaint.alpha = (80 - i * 25).coerceAtLeast(30)
            val path = Path()
            val sweepAngle = 90f + (amplitude * 60f).coerceAtMost(90f)
            val startAngle = rotationAngle + i * 40f
            path.addArc(cx - r, cy - r, cx + r, cy + r, startAngle, sweepAngle)
            canvas.drawPath(path, ringPaint)
        }

        wavePaint.color = accentColor
        val waveRadius = radius * 1.7f
        for (i in 0..2) {
            wavePaint.alpha = (60 + (amplitude * 100).toInt()).coerceAtMost(180)
            val path = Path()
            var first = true
            for (angle in 0..360 step 5) {
                val rad = Math.toRadians(angle.toDouble())
                val wr = waveRadius + sin(angle * 0.05 + waveOffset * 2 * Math.PI + i) * (10 * amplitude)
                val x = cx + wr * cos(rad)
                val y = cy + wr * sin(rad)
                if (first) { path.moveTo(x.toFloat(), y.toFloat()); first = false }
                else path.lineTo(x.toFloat(), y.toFloat())
            }
            path.close()
            canvas.drawPath(path, wavePaint)
        }

        particlePaint.color = accentColor
        val particleRadius = radius * 1.9f
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 + rotationAngle).toDouble())
            val px = cx + particleRadius * cos(angle)
            val py = cy + particleRadius * sin(angle)
            particlePaint.alpha = (150 + (amplitude * 100).toInt()).coerceAtMost(255)
            canvas.drawCircle(px.toFloat(), py.toFloat(), 3f + amplitude * 2, particlePaint)
        }
    }
}
