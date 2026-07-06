package com.pttlan.core.network.protocol

import kotlinx.serialization.Serializable

@Serializable
enum class AudioCodecType {
    PCM16,
    OPUS,
}

@Serializable
data class AudioEnvelope(
    val channelId: String,
    val senderId: String,
    val sequenceNumber: Int,
    val codec: AudioCodecType,
    val timestampMs: Long,
)
