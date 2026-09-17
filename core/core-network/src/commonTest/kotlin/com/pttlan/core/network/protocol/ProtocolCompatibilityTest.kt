package com.pttlan.core.network.protocol

import com.pttlan.core.network.PttJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 21.5: both sides share `PttJson`, so a field added by a newer peer must not break an older one.
 */
class ProtocolCompatibilityTest {
    @Test
    fun ignoresFieldsAddedByANewerPeer() {
        val fromTheFuture =
            """{"type":"join_channel","channelId":"c1","nickname":"Tester","userId":"u1","priority":3}"""

        val message = PttJson.decodeFromString<ControlMessage>(fromTheFuture)

        assertEquals(ControlMessage.JoinChannel("c1", "Tester", "u1"), message)
    }

    @Test
    fun keepsControlFramesSmall() {
        val encoded = PttJson.encodeToString<ControlMessage>(ControlMessage.FloorDenied("c1", "Alguém já está falando"))

        assertTrue(encoded.length < 100, "control frames should stay compact: $encoded")
    }
}
