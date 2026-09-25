package com.pttlan.core.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

private const val RATE = 48_000
private const val TONE_HZ = 220.0

class TimeStretcherTest {
    /** Two seconds of a 220 Hz tone as 16 bit little-endian PCM, the format the history stores. */
    private val tone =
        ByteArray(RATE * 2 * 2).also { pcm ->
            for (i in 0 until RATE * 2) {
                val sample = (sin(2 * PI * TONE_HZ * i / RATE) * 10_000).roundToInt()
                pcm[2 * i] = sample.toByte()
                pcm[2 * i + 1] = (sample shr 8).toByte()
            }
        }

    /** Feeds the input in the history's 4096 byte chunks, then flushes. */
    private fun stretch(speed: Float): ByteArray {
        val stretcher = TimeStretcher(RATE).also { it.speed = speed }
        val out = mutableListOf<Byte>()
        tone.toList().chunked(4096).forEach { out += stretcher.process(it.toByteArray()).toList() }
        out += stretcher.flush().toList()
        return out.toByteArray()
    }

    /** Frequency from upward zero crossings, away from the faded edges. */
    private fun frequencyOf(pcm: ByteArray): Double {
        val samples = (0 until pcm.size / 2).map { (pcm[2 * it].toInt() and 0xFF) or (pcm[2 * it + 1].toInt() shl 8) }
        val middle = samples.subList(samples.size / 10, samples.size * 9 / 10)
        val crossings = middle.zipWithNext().count { (a, b) -> a < 0 && b >= 0 }
        return crossings * RATE.toDouble() / middle.size
    }

    @Test
    fun normalSpeedPassesTheAudioThroughUntouched() {
        assertContentEquals(tone, stretch(1f))
    }

    @Test
    fun doubleSpeedHalvesTheDurationAndKeepsThePitch() {
        val out = stretch(2f)

        val ratio = out.size.toDouble() / tone.size
        assertTrue(abs(ratio - 0.5) < 0.02, "expected half the length, got $ratio")
        val hz = frequencyOf(out)
        assertTrue(abs(hz - TONE_HZ) < TONE_HZ * 0.03, "the voice must keep its pitch, got $hz Hz")
    }

    @Test
    fun oneAndAHalfSpeedShortensByAThird() {
        val out = stretch(1.5f)

        val ratio = out.size.toDouble() / tone.size
        assertTrue(abs(ratio - 1 / 1.5) < 0.02, "expected two thirds of the length, got $ratio")
        val hz = frequencyOf(out)
        assertTrue(abs(hz - TONE_HZ) < TONE_HZ * 0.03, "the voice must keep its pitch, got $hz Hz")
    }
}
