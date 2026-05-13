package com.portfolio.snorerecoder

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission

class AudioRecorder(
    private val sampleRate: Int = 16000,
    private val onAudioChunk: (FloatArray) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    // YAMNet chunk size: 15600 samples
    private val chunkSize = 15600

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 4
        )

        audioRecord?.startRecording()
        isRecording = true

        Thread {
            val shortBuffer = ShortArray(chunkSize)
            while (isRecording) {
                val read = audioRecord?.read(shortBuffer, 0, chunkSize) ?: 0
                if (read > 0) {
                    // Convert Short PCM to normalized Float [-1.0, 1.0]
                    val floatBuffer = FloatArray(read) { i ->
                        shortBuffer[i] / 32768.0f
                    }
                    onAudioChunk(floatBuffer)
                }
            }
        }.start()
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}