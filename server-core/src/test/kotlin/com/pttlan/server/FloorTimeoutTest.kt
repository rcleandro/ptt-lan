package com.pttlan.server

import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.server.auth.JwtConfig
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds

/**
 * Phase 20.4: a speaker that stops sending audio — app killed, Wi-Fi gone — used to hold the floor until the
 * ping timeout (~35s). The channel now takes it back after `ptt.floorIdleTimeoutMs`.
 */
class FloorTimeoutTest {
    @Test
    fun releasesTheFloorWhenTheSpeakerGoesSilent() =
        testApplication {
            environment {
                config =
                    ApplicationConfig("application.conf").mergeWith(
                        MapApplicationConfig("ptt.floorIdleTimeoutMs" to "300"),
                    )
            }
            val client = createClient { install(WebSockets) }
            val token = JwtConfig.generateToken("u1", "Tester", "device-1")

            client.webSocket("/ws?token=$token") {
                send(Frame.Text(Json.encodeToString<ControlMessage>(ControlMessage.JoinChannel("c1", "Tester", "u1"))))
                send(Frame.Text(Json.encodeToString<ControlMessage>(ControlMessage.StartSpeaking("c1", "u1"))))

                // No audio frame follows, so the watchdog should hand the floor back on its own
                val released =
                    withTimeoutOrNull(5.seconds) {
                        incoming
                            .receiveAsFlow()
                            .filterIsInstance<Frame.Text>()
                            .map { Json.decodeFromString<ControlMessage>(it.readText()) }
                            .filterIsInstance<ControlMessage.SpeakerChanged>()
                            .first { !it.isSpeaking }
                    }

                assertNotNull(released, "the floor should be released after the idle timeout")
                assertEquals("u1", released.userId)
            }
        }
}
