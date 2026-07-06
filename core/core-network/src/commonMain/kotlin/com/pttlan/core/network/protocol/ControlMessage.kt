package com.pttlan.core.network.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParticipantDto(
    val userId: String,
    val nickname: String,
    val isSpeaking: Boolean,
)

@Serializable
sealed interface ControlMessage {
    @Serializable
    @SerialName("join_channel")
    data class JoinChannel(
        val channelId: String,
        val nickname: String,
        val userId: String,
    ) : ControlMessage

    @Serializable
    @SerialName("leave_channel")
    data class LeaveChannel(
        val channelId: String,
        val userId: String,
    ) : ControlMessage

    @Serializable
    @SerialName("start_speaking")
    data class StartSpeaking(
        val channelId: String,
        val userId: String,
    ) : ControlMessage

    @Serializable
    @SerialName("stop_speaking")
    data class StopSpeaking(
        val channelId: String,
        val userId: String,
    ) : ControlMessage

    @Serializable
    @SerialName("participant_list")
    data class ParticipantList(
        val channelId: String,
        val participants: List<ParticipantDto>,
    ) : ControlMessage

    @Serializable
    @SerialName("speaker_changed")
    data class SpeakerChanged(
        val channelId: String,
        val userId: String,
        val isSpeaking: Boolean,
    ) : ControlMessage

    @Serializable
    @SerialName("floor_denied")
    data class FloorDenied(
        val channelId: String,
        val reason: String,
    ) : ControlMessage

    @Serializable
    @SerialName("heartbeat")
    data class Heartbeat(
        val userId: String,
    ) : ControlMessage
}
