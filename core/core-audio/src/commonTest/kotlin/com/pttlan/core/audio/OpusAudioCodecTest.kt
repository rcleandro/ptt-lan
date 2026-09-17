package com.pttlan.core.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SAMPLE_RATE = 48_000

/** 20 ms at 48 kHz. Opus only accepts 120/240/480/960/1920/2880 samples per frame. */
private const val FRAME_SAMPLES = 960

class OpusAudioCodecTest {
    private fun tone(samples: Int): ByteArray {
        val bytes = ByteArray(samples * 2)
        for (i in 0 until samples) {
            val value = (sin(2.0 * PI * 440.0 * i / SAMPLE_RATE) * Short.MAX_VALUE).toInt()
            bytes[i * 2] = (value and 0xFF).toByte()
            bytes[i * 2 + 1] = ((value shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    @Test
    fun encodesAValidTwentyMillisecondFrame() {
        val encoded = OpusAudioCodec().encode(tone(FRAME_SAMPLES))

        assertTrue(encoded.isNotEmpty(), "a 960 sample frame must produce Opus data")
    }

    @Test
    fun roundTripKeepsTheFrameLength() {
        val codec = OpusAudioCodec()

        val decoded = codec.decode(codec.encode(tone(FRAME_SAMPLES)))

        assertEquals(FRAME_SAMPLES * 2, decoded.size)
    }

    @Test
    fun returnsEmptyForAFrameSizeOpusRejects() {
        // The size the old iOS tap produced; the caller must notice and skip the frame instead of sending it
        val encoded = OpusAudioCodec().encode(tone(2048))

        assertTrue(encoded.isEmpty(), "an invalid frame size must not pass as encoded audio")
    }
}
