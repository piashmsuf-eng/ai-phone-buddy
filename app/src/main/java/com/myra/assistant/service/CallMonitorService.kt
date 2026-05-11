package com.myra.assistant.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat

class CallMonitorService : Service() {
    private lateinit var telephonyManager: TelephonyManager
    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    val name = resolveCallerName(phoneNumber)
                    val intent = Intent(this@CallMonitorService, Class.forName("com.myra.assistant.ui.main.MainActivity")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("INCOMING_CALL", true)
                        putExtra("CALLER_NAME", name ?: phoneNumber ?: "Unknown")
                    }
                    startActivity(intent)
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    sendBroadcast(Intent("com.myra.CALL_ENDED"))
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("myra_call", "Call Monitor", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(this, "myra_call")
            .setContentTitle("MYRA Call Monitor")
            .setContentText("Active")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .build()
        startForeground(2001, notif)
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun resolveCallerName(number: String?): String? {
        if (number.isNullOrBlank()) return null
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        val last7 = number.takeLast(7)
        contentResolver.query(uri, projection, selection, arrayOf("%$last7"), null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        super.onDestroy()
    }
}
