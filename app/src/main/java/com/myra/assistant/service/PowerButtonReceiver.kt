package com.myra.assistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PowerButtonReceiver : BroadcastReceiver() {
    companion object {
        private var lastPressTime = 0L
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_SCREEN_OFF || action == Intent.ACTION_SCREEN_ON) {
            val now = System.currentTimeMillis()
            if (now - lastPressTime < 600) {
                Log.d("MYRA", "Double press detected")
                val serviceIntent = Intent(context, MyraOverlayService::class.java)
                context.startService(serviceIntent)
            }
            lastPressTime = now
        }
    }
}
