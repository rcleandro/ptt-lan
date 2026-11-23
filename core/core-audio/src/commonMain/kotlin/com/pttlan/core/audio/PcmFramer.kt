package com.pttlan.core.audio

/**
 * Cuts captured PCM into frames of exactly [frameBytes]: a microphone delivers whatever block size the hardware
 * picked, and Opus only takes whole frames. The rest waits for the next block.
 *
 * Not thread-safe on purpose: each capture owns its own framer, used only by the thread that delivers the audio.
 */
class PcmFramer(
    private val frameBytes: Int,
) {
    private var pending = ByteArray(0)

    fun push(bytes: ByteArray): List<ByteArray> {
        pending += bytes
        val frames = ArrayList<ByteArray>(pending.size / frameBytes)
        var start = 0
        while (pending.size - start >= frameBytes) {
            frames += pending.copyOfRange(start, start + frameBytes)
            start += frameBytes
        }
        pending = pending.copyOfRange(start, pending.size)
        return frames
    }
}
