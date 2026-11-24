package com.pttlan.server

import com.pttlan.core.network.protocol.LoginRequest
import com.pttlan.core.network.protocol.LoginResponse
import com.pttlan.server.channel.Participant
import com.pttlan.server.channel.PttChannel
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Achado 3 da rodada da 29.1: a server restarted with a new signing key refuses the old token, and the client logs in
 * again. For that to keep the session, one device must get the same user id on every login.
 */
class StableUserIdTest {
    @Test
    fun oneDeviceGetsTheSameUserIdOnEveryLogin() =
        testApplication {
            application { module() }
            val client = createClient { install(ContentNegotiation) { json() } }

            suspend fun login(deviceId: String) =
                client
                    .post("/api/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("Tester", deviceId))
                    }.body<LoginResponse>()
                    .userId

            val first = login("device-1")
            assertEquals(first, login("device-1"))
            assertNotEquals(first, login("device-2"))
        }

    @Test
    fun aReplacedSessionLeavingLateDoesNotTakeOutTheNewOne() =
        runTest {
            val channel = PttChannel("Geral")
            val oldSession = mockk<DefaultWebSocketServerSession>(relaxed = true)
            val newSession = mockk<DefaultWebSocketServerSession>(relaxed = true)
            channel.addParticipant(Participant("u1", "Tester", oldSession))
            channel.addParticipant(Participant("u1", "Tester", newSession))

            channel.removeParticipant("u1", oldSession)
            assertEquals(1, channel.participantCount, "the old session's cleanup must leave the new one in")

            channel.removeParticipant("u1", newSession)
            assertEquals(0, channel.participantCount)
        }
}
