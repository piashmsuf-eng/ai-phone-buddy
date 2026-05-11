package com.myra.assistant.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import androidx.core.app.NotificationCompat

class MyraOverlayService : Service() {
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    companion object { var isRunning = false }

    override fun onCreate() {
        super.onCreate()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel("myra_overlay", "MYRA Overlay", NotificationManager.IMPORTANCE_LOW)
                try { getSystemService(NotificationManager::class.java).createNotificationChannel(channel) } catch (_: Exception) {}
            }
            val pi = PendingIntent.getActivity(this, 0,
                Intent(this, Class.forName("com.myra.assistant.ui.main.MainActivity")),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val notif = NotificationCompat.Builder(this, "myra_overlay")
                .setContentTitle("MYRA")
                .setContentText("Overlay active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            try { startForeground(2002, notif) } catch (e: Exception) {
                Log.e("MYRA_SVC", "startForeground failed", e)
                stopSelf()
                return
            }
            isRunning = true
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            try { createOverlay() } catch (e: Exception) {
                Log.e("MYRA_SVC", "createOverlay failed", e)
            }
        } catch (e: Exception) {
            Log.e("MYRA_SVC", "onCreate fatal", e)
            try { stopSelf() } catch (_: Exception) {}
        }
    }

    private fun createOverlay() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layoutId = resources.getIdentifier("overlay_orb", "layout", packageName)
        overlayView = inflater.inflate(layoutId, null)
        val params = WindowManager.LayoutParams(
            300, 400,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        windowManager?.addView(overlayView, params)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try { overlayView?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        isRunning = false
        super.onDestroy()
    }
}
