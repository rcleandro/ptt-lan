package com.pttlan.core.network.protocol

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControlMessageTest {
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            classDiscriminator = "type"
        }

    @Test
    fun `test JoinChannel round trip serialization`() {
        val original =
            ControlMessage.JoinChannel(
                channelId = "123",
                nickname = "Leandro",
                userId = "user_456",
            )

        val jsonString = json.encodeToString<ControlMessage>(original)
        assertTrue(jsonString.contains("join_channel"))

        val deserialized = json.decodeFromString<ControlMessage>(jsonString)

        assertEquals(original, deserialized)
    }

    @Test
    fun `test ParticipantList round trip serialization`() {
        val original =
            ControlMessage.ParticipantList(
                channelId = "123",
                participants =
                    listOf(
                        ParticipantDto(userId = "user_1", nickname = "Alice", isSpeaking = true),
                        ParticipantDto(userId = "user_2", nickname = "Bob", isSpeaking = false),
                    ),
            )

        val jsonString = json.encodeToString<ControlMessage>(original)
        val deserialized = json.decodeFromString<ControlMessage>(jsonString)

        assertEquals(original, deserialized)
    }
}
