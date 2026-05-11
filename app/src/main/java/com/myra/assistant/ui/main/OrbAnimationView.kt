package com.myra.assistant.ui.main

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.jvm.JvmOverloads
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState { IDLE, LISTENING, SPEAKING, THINKING }

class OrbAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var state = OrbState.IDLE
        set(value) { field = value; updateColors(); safeInvalidate() }
    var amplitude = 0f

    private var ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    private var corePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
    private var rotationAngle = 0f
    private var pulseScale = 1f
    private var waveOffset = 0f
    private var coreColor = 0xFFB71C1C.toInt()
    private var accentColor = 0xFFFF1744.toInt()

    private var rotateAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var waveAnimator: ValueAnimator? = null

    init {
        try {
            rotateAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 3000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
                addUpdateListener { rotationAngle = it.animatedValue as Float; safeInvalidate() }
            }
            pulseAnimator = ValueAnimator.ofFloat(1f, 1.15f).apply {
                duration = 1500; repeatMode = ValueAnimator.REVERSE; repeatCount = ValueAnimator.INFINITE
                addUpdateListener { pulseScale = it.animatedValue as Float; safeInvalidate() }
            }
            waveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1200; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
                addUpdateListener { waveOffset = it.animatedValue as Float; safeInvalidate() }
            }
        } catch (e: Exception) {
            Log.e("MYRA_ORB", "init animators failed", e)
        }
    }

    private fun safeInvalidate() {
        try { invalidate() } catch (_: Exception) {}
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        try { rotateAnimator?.start() } catch (_: Exception) {}
        try { pulseAnimator?.start() } catch (_: Exception) {}
        try { waveAnimator?.start() } catch (_: Exception) {}
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try { rotateAnimator?.cancel() } catch (_: Exception) {}
        try { pulseAnimator?.cancel() } catch (_: Exception) {}
        try { waveAnimator?.cancel() } catch (_: Exception) {}
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
        try {
            drawOrb(canvas)
        } catch (e: Exception) {
            Log.e("MYRA_ORB", "onDraw crash", e)
        }
    }

    private fun drawOrb(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val cx = w / 2f; val cy = h / 2f
        val radius = (minOf(w, h) / 2.8f) * pulseScale

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
