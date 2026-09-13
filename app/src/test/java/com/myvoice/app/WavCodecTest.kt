package com.myvoice.app

import com.myvoice.app.audio.WavCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WavCodecTest {

    @Test
    fun `header contains RIFF and WAVE markers`() {
        val wav = WavCodec.pcmToWav(ByteArray(100), sampleRate = 16000)
        val riff = String(wav.copyOfRange(0, 4), Charsets.US_ASCII)
        val wave = String(wav.copyOfRange(8, 12), Charsets.US_ASCII)
        val fmt = String(wav.copyOfRange(12, 16), Charsets.US_ASCII)
        val data = String(wav.copyOfRange(36, 40), Charsets.US_ASCII)
        assertEquals("RIFF", riff)
        assertEquals("WAVE", wave)
        assertEquals("fmt ", fmt)
        assertEquals("data", data)
    }

    @Test
    fun `sizes are little-endian and correct`() {
        val pcm = ByteArray(3200) // 100ms of 16kHz mono 16-bit
        val wav = WavCodec.pcmToWav(pcm, sampleRate = 16000)
        assertEquals(44 + 3200, wav.size)
        // RIFF chunk size = 36 + pcm.size, little-endian at offset 4
        val riffSize = readLe32(wav, 4)
        assertEquals(36 + 3200, riffSize)
        // data chunk size at offset 40
        val dataSize = readLe32(wav, 40)
        assertEquals(3200, dataSize)
        // sample rate at offset 24
        assertEquals(16000, readLe32(wav, 24))
    }

    @Test
    fun `duration math is correct`() {
        // 16000 samples/s * 2 bytes = 32000 bytes per second
        assertEquals(1000L, WavCodec.pcmDurationMs(32000, 16000))
        assertEquals(500L, WavCodec.pcmDurationMs(16000, 16000))
        assertEquals(0L, WavCodec.pcmDurationMs(16000, 0))
        assertEquals(0L, WavCodec.pcmDurationMs(0, 16000))
    }

    @Test
    fun `wav is larger than pcm by header size`() {
        val pcm = ByteArray(1024)
        assertTrue(WavCodec.pcmToWav(pcm, 16000).size == pcm.size + 44)
    }

    private fun readLe32(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
}
