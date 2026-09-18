package com.pttlan.core.audio

/** Default packets to queue before playback starts: ~100 ms at 20 ms frames. */
const val DEFAULT_PREBUFFER_PACKETS = 5

/** How far back a sequence number may jump before it is treated as a new talk spurt instead of a late packet. */
const val DEFAULT_RESYNC_WINDOW = 20

data class AudioPacket(
    val chunk: ByteArray,
    val sequenceNumber: Int,
    val timestampMs: Long,
) : Comparable<AudioPacket> {
    override fun compareTo(other: AudioPacket): Int = sequenceNumber.compareTo(other.sequenceNumber)

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is AudioPacket && sequenceNumber == other.sequenceNumber && timestampMs == other.timestampMs)

    override fun hashCode(): Int = 31 * sequenceNumber + timestampMs.hashCode()
}

/**
 * Ordering and discard policy of the playback queue, shared by every platform: the platforms only own the
 * queue and the device write (`AudioTrack`, `SourceDataLine`). It holds no locks — it is meant to be used
 * from the playback loop only, which is where the sequence state already lived.
 */
class JitterBufferPolicy(
    private val prebufferPackets: Int = DEFAULT_PREBUFFER_PACKETS,
    private val resyncWindow: Int = DEFAULT_RESYNC_WINDOW,
) {
    private var expectedSequenceNumber = -1
    private var buffering = true

    /** True while too few packets are queued to start playing without stuttering. */
    fun shouldWaitForMore(queueSize: Int): Boolean {
        if (!buffering) return false
        if (queueSize < prebufferPackets) return true
        buffering = false
        return false
    }

    /** Whether this packet goes to the device. A packet that arrives after its turn is dropped. */
    fun shouldPlay(sequenceNumber: Int): Boolean {
        val isNewSpurt = expectedSequenceNumber == -1 || sequenceNumber < expectedSequenceNumber - resyncWindow
        if (isNewSpurt) {
            expectedSequenceNumber = sequenceNumber
        }
        if (sequenceNumber < expectedSequenceNumber) return false
        expectedSequenceNumber = sequenceNumber + 1
        return true
    }

    /** Nothing arrived in time: pre-buffer again and resync on the next packet. */
    fun onStarved() {
        buffering = true
        expectedSequenceNumber = -1
    }
}
