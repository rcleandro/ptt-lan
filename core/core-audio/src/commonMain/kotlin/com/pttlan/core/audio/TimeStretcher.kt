package com.pttlan.core.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val FRAMES_PER_SECOND = 50 // 20 ms frames
private const val TOLERANCES_PER_SECOND = 200 // search ±5 ms
private const val SEARCH_STRIDE = 2

/**
 * Plays speech faster without raising its pitch, for the history's 1.5× and 2× (WSOLA). The input is cut in
 * 20 ms frames that overlap by half; at speed s each frame starts s times further into the input than it lands
 * in the output, nudged by up to 5 ms to wherever it lines up best with the end of the previous frame, so the
 * overlap-add does not beat. Pure Kotlin, so it is the same on every platform.
 *
 * Takes and returns 16 bit little-endian mono PCM, in chunks of any size. It keeps up to a frame of input
 * between calls; [flush] returns the last of it when the message ends. Not thread safe.
 */
class TimeStretcher(
    sampleRate: Int = 48_000,
) {
    /** Output speed; 1 plays the audio untouched until another speed is set. */
    var speed: Float = 1f

    private val frame = sampleRate / FRAMES_PER_SECOND
    private val hop = frame / 2
    private val tolerance = sampleRate / TOLERANCES_PER_SECOND

    // Periodic Hann: two halves that overlap by half add up to exactly 1
    private val window = FloatArray(frame) { 0.5f - 0.5f * cos(2 * PI * it / frame).toFloat() }

    private var input = FloatArray(0)
    private var nominal = 0.0
    private var previous = 0
    private var started = false
    private val tail = FloatArray(hop)

    fun process(pcm: ByteArray): ByteArray {
        if (speed == 1f && !started) return pcm

        input += pcm.toSamples()
        val out = ArrayList<Float>()
        while (nominal.toInt() + tolerance + frame <= input.size) {
            // Where the previous frame would have gone on: its start can sit before the input kept, never its end
            val start = if (started) bestMatch(previous + hop, nominal.toInt()) else nominal.toInt()
            for (i in 0 until hop) out += tail[i] + input[start + i] * window[i]
            for (i in 0 until hop) tail[i] = input[start + hop + i] * window[hop + i]
            previous = start
            started = true
            nominal += hop * speed
            dropConsumedInput()
        }
        return out.toPcm()
    }

    /** The overlap still waiting for a next frame. The stretcher starts over afterwards. */
    fun flush(): ByteArray {
        if (!started) return ByteArray(0)
        val out = tail.toList().toPcm()
        input = FloatArray(0)
        nominal = 0.0
        previous = 0
        started = false
        tail.fill(0f)
        return out
    }

    /** The start within ±tolerance of [center] whose opening best continues the input at [natural]. */
    private fun bestMatch(
        natural: Int,
        center: Int,
    ): Int {
        var best = center
        var bestScore = Double.NEGATIVE_INFINITY
        for (candidate in maxOf(0, center - tolerance)..center + tolerance) {
            var dot = 0.0
            var energy = 0.0
            for (i in 0 until hop step SEARCH_STRIDE) {
                val sample = input[candidate + i]
                dot += sample * input[natural + i]
                energy += sample * sample
            }
            val score = if (energy > 0) dot / sqrt(energy) else 0.0
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }
        return best
    }

    private fun dropConsumedInput() {
        val drop = minOf(previous + hop, nominal.toInt() - tolerance)
        if (drop <= 0) return
        input = input.copyOfRange(drop, input.size)
        previous -= drop
        nominal -= drop
    }
}

private fun ByteArray.toSamples(): FloatArray = toShortArray().let { shorts -> FloatArray(shorts.size) { shorts[it].toFloat() } }

private fun List<Float>.toPcm(): ByteArray =
    ShortArray(size) { this[it].roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort() }.toByteArray()
