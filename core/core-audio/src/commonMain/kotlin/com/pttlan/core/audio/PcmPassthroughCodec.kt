package com.pttlan.core.audio

class PcmPassthroughCodec : AudioCodec {
    override fun encode(pcm: ByteArray): ByteArray {
        return pcm
    }

    override fun decode(encoded: ByteArray): ByteArray {
        return encoded
    }
}
