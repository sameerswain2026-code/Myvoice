package com.myvoice.app.audio

import java.io.ByteArrayOutputStream

/**
 * Pure helpers for building WAV (RIFF) containers around raw 16-bit PCM.
 */
object WavCodec {

    fun pcmToWav(
        pcm: ByteArray,
        sampleRate: Int,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        val out = ByteArrayOutputStream(44 + pcm.size)
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        fun le16(v: Int) {
            out.write(v and 0xFF)
            out.write((v shr 8) and 0xFF)
        }

        fun le32(v: Int) {
            out.write(v and 0xFF)
            out.write((v shr 8) and 0xFF)
            out.write((v shr 16) and 0xFF)
            out.write((v shr 24) and 0xFF)
        }

        out.write("RIFF".toByteArray(Charsets.US_ASCII))
        le32(36 + pcm.size)
        out.write("WAVE".toByteArray(Charsets.US_ASCII))
        out.write("fmt ".toByteArray(Charsets.US_ASCII))
        le32(16) // PCM chunk size
        le16(1)  // audio format = PCM
        le16(channels)
        le32(sampleRate)
        le32(byteRate)
        le16(blockAlign)
        le16(bitsPerSample)
        out.write("data".toByteArray(Charsets.US_ASCII))
        le32(pcm.size)
        out.write(pcm)
        return out.toByteArray()
    }

    fun pcmDurationMs(
        pcmBytes: Int,
        sampleRate: Int,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): Long {
        if (sampleRate <= 0) return 0
        val bytesPerSecond = sampleRate.toLong() * channels * bitsPerSample / 8
        if (bytesPerSecond <= 0) return 0
        return pcmBytes.toLong() * 1000L / bytesPerSecond
    }
}
