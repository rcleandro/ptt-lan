package com.pttlan.server

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

/** Room PIN (24.3): a hosted room only lets in who knows the PIN; a server without one stays open. */
class RoomPinTest {
    private fun ApplicationTestBuilder.withRoomPin(pin: String?) {
        environment {
            config =
                ApplicationConfig("application.conf").mergeWith(
                    MapApplicationConfig("ptt.roomPin" to pin.orEmpty()),
                )
        }
    }

    private suspend fun ApplicationTestBuilder.login(pinField: String) =
        client
            .post("/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"nickname":"User","deviceId":"d1"$pinField}""")
            }.status

    @Test
    fun `with a pin, login needs the right one`() =
        testApplication {
            withRoomPin("482193")

            assertEquals(HttpStatusCode.Unauthorized, login(""))
            assertEquals(HttpStatusCode.Unauthorized, login(""","pin":"0000""""))
            assertEquals(HttpStatusCode.OK, login(""","pin":"482193""""))
        }

    @Test
    fun `without a pin, login stays open and ignores the field`() =
        testApplication {
            withRoomPin(null)

            assertEquals(HttpStatusCode.OK, login(""))
            assertEquals(HttpStatusCode.OK, login(""","pin":"whatever""""))
        }
}
