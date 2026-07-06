package com.pttlan.core.audio

class PcmPassthroughCodec : AudioCodec {
    override fun encode(pcm: ByteArray): ByteArray = pcm

    override fun decode(encoded: ByteArray): ByteArray = encoded
}
