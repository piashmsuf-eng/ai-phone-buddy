package com.myra.assistant.ai

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveClient(
    private val apiKey: String,
    private val modelName: String,
    private val voiceName: String,
    private val systemPrompt: String
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var reconnectJob: Job? = null
    private var keepAliveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val WS_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
    private var sessionStartTime = 0L

    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onAudioReceived: ((ByteArray) -> Unit)? = null
    var onOutputTranscript: ((String) -> Unit)? = null
    var onInputTranscript: ((String) -> Unit)? = null
    var onTurnComplete: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun connect() {
        val request = Request.Builder().url(WS_URL).build()
        sessionStartTime = System.currentTimeMillis()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("GeminiLive", "Connected")
                val tool = JSONObject().apply {
                    put("functionDeclarations", JSONArray().apply {
                        put(JSONObject().apply {
                            put("name", "control_phone")
                            put("description", "Control Android device")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("action", JSONObject().apply {
                                        put("type", "STRING")
                                        put("enum", JSONArray(listOf("open_app", "close_app", "call", "sms", "whatsapp", "volume_up", "volume_down", "flashlight_on", "flashlight_off", "wifi_on", "wifi_off", "bluetooth_on", "bluetooth_off")))
                                    })
                                    put("target", JSONObject().apply { put("type", "STRING") })
                                    put("message", JSONObject().apply { put("type", "STRING") })
                                })
                                put("required", JSONArray(listOf("action")))
                            })
                        })
                    })
                }
                val setup = JSONObject().apply {
                    put("setup", JSONObject().apply {
                        put("model", modelName)
                        put("system_instruction", JSONObject().apply {
                            put("parts", JSONArray().apply { put(JSONObject().apply { put("text", systemPrompt) }) })
                        })
                        put("tools", JSONArray().apply { put(tool) })
                        put("generation_config", JSONObject().apply {
                            put("response_modalities", JSONArray(listOf("AUDIO")))
                            put("speech_config", JSONObject().apply {
                                put("voice_config", JSONObject().apply {
                                    put("prebuilt_voice_config", JSONObject().apply {
                                        put("voice_name", voiceName)
                                    })
                                })
                            })
                            put("temperature", 0.9)
                        })
                        put("output_audio_transcription", JSONObject())
                        put("input_audio_transcription", JSONObject())
                    })
                }
                ws.send(setup.toString())
                onConnected?.invoke()
                startKeepAlive(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val msg = JSONObject(text)
                    if (msg.has("setupComplete")) {
                        sessionStartTime = System.currentTimeMillis()
                        return
                    }
                    val serverContent = msg.optJSONObject("serverContent") ?: return
                    val modelTurn = serverContent.optJSONObject("modelTurn") ?: return
                    val parts = modelTurn.optJSONArray("parts") ?: return
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val inlineData = part.optJSONObject("inlineData")
                        if (inlineData != null) {
                            val b64 = inlineData.optString("data", "")
                            if (b64.isNotBlank()) {
                                val audioBytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                onAudioReceived?.invoke(audioBytes)
                            }
                        }
                    }
                    val outputTranscription = serverContent.optJSONObject("outputTranscription")
                    if (outputTranscription != null) {
                        val text2 = outputTranscription.optString("text", "")
                        if (text2.isNotBlank()) onOutputTranscript?.invoke(text2)
                    }
                    val inputTranscription = serverContent.optJSONObject("inputTranscription")
                    if (inputTranscription != null) {
                        val text3 = inputTranscription.optString("text", "")
                        if (text3.isNotBlank()) onInputTranscript?.invoke(text3)
                    }
                    if (serverContent.optBoolean("turnComplete", false)) {
                        onTurnComplete?.invoke()
                    }
                } catch (e: Exception) {
                    Log.e("GeminiLive", "Parse error", e)
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
                onDisconnected?.invoke()
                tryReconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("GeminiLive", "Failure", t)
                onError?.invoke(t.message ?: "Connection failed")
                onDisconnected?.invoke()
                tryReconnect()
            }
        })
    }

    fun sendAudio(audioData: ByteArray) {
        val b64 = android.util.Base64.encodeToString(audioData, android.util.Base64.NO_WRAP)
        val msg = JSONObject().apply {
            put("realtime_input", JSONObject().apply {
                put("media_chunks", JSONArray().apply {
                    put(JSONObject().apply {
                        put("mime_type", "audio/pcm;rate=16000")
                        put("data", b64)
                    })
                })
            })
        }
        webSocket?.send(msg.toString())
    }

    fun sendText(text: String) {
        Log.d("GeminiLive", "Sending text: ${text.take(100)}")
        val msg = JSONObject().apply {
            put("client_content", JSONObject().apply {
                put("turns", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply { put(JSONObject().apply { put("text", text) }) })
                    })
                })
                put("turn_complete", true)
            })
        }
        webSocket?.send(msg.toString())
    }

    fun interrupt() {
        val msg = JSONObject().apply {
            put("client_content", JSONObject().apply {
                put("turns", JSONArray())
                put("turn_complete", true)
            })
        }
        webSocket?.send(msg.toString())
    }

    fun disconnect() {
        keepAliveJob?.cancel()
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }

    private fun startKeepAlive(ws: WebSocket) {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive) {
                delay(8000)
                if (System.currentTimeMillis() - sessionStartTime > 540_000) {
                    Log.d("GeminiLive", "Session renew")
                    ws.close(1000, "Session renew")
                    delay(3000)
                    connect()
                    return@launch
                }
                val silence = ByteArray(1024)
                val b64 = android.util.Base64.encodeToString(silence, android.util.Base64.NO_WRAP)
                val msg = JSONObject().apply {
                    put("realtime_input", JSONObject().apply {
                        put("media_chunks", JSONArray().apply {
                            put(JSONObject().apply {
                                put("mime_type", "audio/pcm;rate=16000")
                                put("data", b64)
                            })
                        })
                    })
                }
                runCatching { ws.send(msg.toString()) }
            }
        }
    }

    private fun tryReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000)
            Log.d("GeminiLive", "Reconnecting...")
            connect()
        }
    }
}
