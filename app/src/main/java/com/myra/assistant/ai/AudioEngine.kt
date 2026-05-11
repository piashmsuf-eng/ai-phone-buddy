package com.myra.assistant.ai

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.sqrt

class AudioEngine(private val audioManager: AudioManager?) {
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private var isMuted = false
    private val playbackQueue = ConcurrentLinkedQueue<ByteArray>()
    private var isSpeaking = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var onAmplitudeChanged: ((Float) -> Unit)? = null
    var onAudioData: ((ByteArray) -> Unit)? = null
    var onSpeakingStarted: (() -> Unit)? = null
    var onSpeakingStopped: (() -> Unit)? = null
    var onPlaybackComplete: (() -> Unit)? = null

    fun startRecording() {
        val bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            16000, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2
        )
        audioRecord?.startRecording()
        isRecording = true
        scope.launch {
            val buffer = ByteArray(1024)
            while (isRecording && isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if (read > 0 && !isMuted) {
                    val chunk = buffer.copyOf(read)
                    onAudioData?.invoke(chunk)
                    var sum = 0L
                    for (i in 0 until read step 2) {
                        val sample = ((chunk[i + 1].toInt() shl 8) or (chunk[i].toInt() and 0xFF)).toShort()
                        sum += (sample * sample).toLong()
                    }
                    val rms = sqrt(sum.toDouble() / (read / 2)).toFloat() / 32768f
                    onAmplitudeChanged?.invoke(rms)
                }
            }
        }
    }

    fun startPlayback() {
        val bufferSize = AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(24000)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build())
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack?.play()
        scope.launch {
            while (isActive) {
                val data = playbackQueue.poll()
                if (data != null) {
                    if (!isSpeaking) {
                        isSpeaking = true
                        withContext(Dispatchers.Main) { onSpeakingStarted?.invoke() }
                    }
                    audioTrack?.write(data, 0, data.size)
                } else {
                    if (isSpeaking) {
                        isSpeaking = false
                        withContext(Dispatchers.Main) {
                            onSpeakingStopped?.invoke()
                            onPlaybackComplete?.invoke()
                        }
                    }
                    delay(10)
                }
            }
        }
    }

    fun queueAudio(data: ByteArray) {
        playbackQueue.add(data)
    }

    fun clearPlayback() {
        playbackQueue.clear()
        audioTrack?.pause()
        audioTrack?.flush()
        audioTrack?.play()
    }

    fun setMuted(muted: Boolean) { isMuted = muted }

    fun release() {
        isRecording = false
        scope.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
