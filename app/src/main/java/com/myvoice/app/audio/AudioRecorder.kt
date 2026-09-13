package com.myvoice.app.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.myvoice.app.core.AssistantException
import java.io.ByteArrayOutputStream

class AudioRecorder(private val context: Context) {

    data class Recording(val wav: ByteArray, val durationMs: Long)

    @Volatile
    private var recording = false

    private var audioRecord: AudioRecord? = null
    private var thread: Thread? = null
    private val buffer = ByteArrayOutputStream()

    val isRecording: Boolean get() = recording

    fun start(sampleRate: Int = SAMPLE_RATE) {
        check(!recording) { "Already recording" }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            throw AssistantException("Microphone permission is required to record.")
        }
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) throw AssistantException("Audio input is not available on this device.")

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf * 4, 16384)
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw AssistantException("Could not initialize the microphone.")
        }

        synchronized(buffer) { buffer.reset() }
        audioRecord = record
        recording = true
        record.startRecording()

        thread = Thread {
            val chunk = ByteArray(2048)
            while (recording) {
                val n = record.read(chunk, 0, chunk.size)
                if (n > 0) synchronized(buffer) { buffer.write(chunk, 0, n) }
            }
        }.also { it.start() }
    }

    fun stop(): Recording {
        recording = false
        thread?.join(1500)
        thread = null
        val record = audioRecord
        audioRecord = null
        try {
            record?.stop()
        } catch (_: Throwable) {
        }
        try {
            record?.release()
        } catch (_: Throwable) {
        }
        val pcm = synchronized(buffer) {
            val bytes = buffer.toByteArray()
            buffer.reset()
            bytes
        }
        return Recording(
            wav = WavCodec.pcmToWav(pcm, SAMPLE_RATE),
            durationMs = WavCodec.pcmDurationMs(pcm.size, SAMPLE_RATE)
        )
    }

    companion object {
        const val SAMPLE_RATE = 16000
    }
}
