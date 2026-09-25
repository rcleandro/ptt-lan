package com.pttlan.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals

class WavHeaderTest {
    private fun ByteArray.text(
        from: Int,
        length: Int,
    ) = decodeToString(from, from + length)

    private fun ByteArray.int(at: Int) = (0 until 4).sumOf { (this[at + it].toInt() and 0xFF) shl (8 * it) }

    private fun ByteArray.short(at: Int) = (this[at].toInt() and 0xFF) or ((this[at + 1].toInt() and 0xFF) shl 8)

    @Test
    fun describesTheHistoryPcmAsAPlainWav() {
        // One second of the history's audio: 48 kHz, mono, 16 bit
        val header = wavHeader(dataBytes = 96_000)

        assertEquals(44, header.size)
        assertEquals("RIFF", header.text(0, 4))
        assertEquals(36 + 96_000, header.int(4), "RIFF size is everything after its first 8 bytes")
        assertEquals("WAVE", header.text(8, 4))
        assertEquals("fmt ", header.text(12, 4))
        assertEquals(16, header.int(16))
        assertEquals(1, header.short(20), "PCM")
        assertEquals(1, header.short(22), "mono")
        assertEquals(48_000, header.int(24))
        assertEquals(96_000, header.int(28), "byte rate")
        assertEquals(2, header.short(32), "block align")
        assertEquals(16, header.short(34), "bits per sample")
        assertEquals("data", header.text(36, 4))
        assertEquals(96_000, header.int(40))
    }
}
