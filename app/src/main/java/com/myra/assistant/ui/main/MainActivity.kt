package com.myra.assistant.ui.main

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.*
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.myra.assistant.ai.AudioEngine
import com.myra.assistant.ai.CommandParser
import com.myra.assistant.ai.GeminiLiveClient
import com.myra.assistant.databinding.ActivityMainBinding
import com.myra.assistant.service.AccessibilityHelperService
import com.myra.assistant.service.CallMonitorService
import com.myra.assistant.service.MyraOverlayService
import com.myra.assistant.ui.settings.SettingsActivity
import com.myra.assistant.viewmodel.MainViewModel
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var audioEngine: AudioEngine
    private var geminiLive: GeminiLiveClient? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isMuted = false
    private var isActiveMode = false
    private val inputBuffer = StringBuilder()
    private val outputBuffer = StringBuilder()
    private val chatAdapter = ChatAdapter()
    private var isLongPressing = false
    private var longPressRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            viewModel = ViewModelProvider(this)[MainViewModel::class.java]
            audioEngine = AudioEngine(getSystemService(AUDIO_SERVICE) as AudioManager)

            setupUI()
            checkPermissions()
            startSystemServices()
            startStatusUpdates()

            mainHandler.postDelayed({ try { initGeminiLive() } catch (e: Exception) { Log.e("MYRA", "init failed", e) } }, 300)

            handleIncomingCallIntent(intent)
        } catch (e: Exception) {
            Log.e("MYRA", "onCreate crash", e)
            Toast.makeText(this, "Startup error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUI() {
        binding.chatRecycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.chatRecycler.adapter = chatAdapter

        binding.micButton.setOnLongClickListener {
            isLongPressing = true
            geminiLive?.interrupt()
            audioEngine.clearPlayback()
            chatAdapter.addMessage(ChatMessage("Stopped", false))
            true
        }

        binding.micButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    longPressRunnable = Runnable {
                        isLongPressing = true
                        geminiLive?.interrupt()
                        audioEngine.clearPlayback()
                    }
                    mainHandler.postDelayed(longPressRunnable!!, 600)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                    if (!isLongPressing) {
                        toggleMute()
                    }
                    isLongPressing = false
                }
            }
            true
        }

        binding.settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun initGeminiLive() {
        val prefs = getSharedPreferences("myra_prefs", MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""
        if (apiKey.isBlank()) {
            chatAdapter.addMessage(ChatMessage("Please add your Gemini API key in Settings.", false))
            return
        }
        val model = prefs.getString("gemini_model", "models/gemini-2.5-flash-native-audio-preview-12-2025") ?: "models/gemini-2.5-flash-native-audio-preview-12-2025"
        val voice = prefs.getString("gemini_voice", "Aoede") ?: "Aoede"
        val name = prefs.getString("user_name", "Boss") ?: "Boss"
        val personality = prefs.getString("personality_mode", "gf") ?: "gf"

        val prompt = when (personality) {
            "professional" -> "You are MYRA, a professional AI assistant. Use formal English only. Be precise and efficient. Max 2 sentences per response. Current time: ${java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())}."
            else -> "You are MYRA, a warm AI companion for ${name}. Speak in Hinglish (Hindi+English mix). Use words like tum, tumhara, haan, acha, bilkul. Be caring and emotionally expressive. Use emojis sparingly. Max 2-3 sentences. Sound natural when speaking aloud. Current time: ${java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())}."
        }

        geminiLive = GeminiLiveClient(apiKey, model, voice, prompt)
        geminiLive?.apply {
            onConnected = {
                Log.d("MYRA", "Connected")
                audioEngine.startRecording()
                audioEngine.startPlayback()
                mainHandler.postDelayed({
                    val greeting = when (personality) {
                        "professional" -> "Good day ${name}. MYRA is online."
                        else -> "Hey ${name}! Main aa gayi hoon."
                    }
                    sendText(greeting)
                }, 600)
            }
            onAudioReceived = { audio -> audioEngine.queueAudio(audio) }
            onOutputTranscript = { text ->
                outputBuffer.append(" ").append(text.trim())
                chatAdapter.addMessage(ChatMessage(text.trim(), false))
            }
            onInputTranscript = { text ->
                inputBuffer.append(" ").append(text.trim())
                chatAdapter.addMessage(ChatMessage(text.trim(), true))
            }
            onTurnComplete = {
                val input = inputBuffer.toString().trim()
                inputBuffer.clear()
                outputBuffer.clear()
                val cmd = CommandParser.parse(input)
                if (cmd != null) {
                    viewModel.executeCommand(cmd)
                    geminiLive?.sendText("Command executed: ${cmd.type}")
                }
            }
            onError = { err ->
                Log.e("MYRA", err)
                mainHandler.post { Toast.makeText(this@MainActivity, err, Toast.LENGTH_SHORT).show() }
            }
            connect()
        }
    }

    private fun toggleMute() {
        isMuted = !isMuted
        audioEngine.setMuted(isMuted)
        binding.micButton.setImageResource(if (isMuted) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
    }

    private fun handleIncomingCallIntent(intent: Intent?) {
        intent?.let {
            if (it.getBooleanExtra("INCOMING_CALL", false)) {
                val callerName = it.getStringExtra("CALLER_NAME") ?: "Unknown"
                val msg = "Sir, ${callerName} ka call aa raha hai. Uthau ya reject karu?"
                chatAdapter.addMessage(ChatMessage("Incoming call: ${callerName}", true))
                geminiLive?.sendText(msg)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingCallIntent(intent)
    }

    private fun startSystemServices() {
        try { startService(Intent(this, MyraOverlayService::class.java)) } catch (_: Exception) {}
        try { startService(Intent(this, CallMonitorService::class.java)) } catch (_: Exception) {}
    }

    private fun startStatusUpdates() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                binding.batteryText.text = "${level}%"
                val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                binding.timeText.text = time
                handler.postDelayed(this, 30000)
            }
        }
        handler.post(runnable)
    }

    private fun checkPermissions() {
        val perms = arrayOf(
            Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE, Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS
        )
        val needed = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        }
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    override fun onPause() { super.onPause(); audioEngine.setMuted(true) }
    override fun onResume() { super.onResume(); if (!isMuted) audioEngine.setMuted(false) }
    override fun onDestroy() {
        super.onDestroy()
        geminiLive?.disconnect()
        audioEngine.release()
    }
}
