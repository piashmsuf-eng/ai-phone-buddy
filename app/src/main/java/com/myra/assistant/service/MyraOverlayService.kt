package com.myra.assistant.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import androidx.core.app.NotificationCompat

class MyraOverlayService : Service() {
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    companion object { var isRunning = false }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("myra_overlay", "MYRA Overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, Class.forName("com.myra.assistant.ui.main.MainActivity")),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notif = NotificationCompat.Builder(this, "myra_overlay")
            .setContentTitle("MYRA Running")
            .setContentText("Double press power for overlay")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        startForeground(2002, notif)
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
    }

    private fun createOverlay() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(resources.getIdentifier("overlay_orb", "layout", packageName), null)
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
        overlayView?.let { windowManager?.removeView(it) }
        isRunning = false
        super.onDestroy()
    }
}
