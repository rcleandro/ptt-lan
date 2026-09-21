package com.pttlan.server

import com.pttlan.core.network.protocol.LoginRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The login limit is 5/min per client IP. Behind a reverse proxy every request carries the proxy's IP,
 * so the limit is only correct when `XForwardedHeaders` is installed — which only happens with `ptt.trustProxy`.
 */
class ProxyRateLimitTest {
    private fun ApplicationTestBuilder.withTrustProxy(trustProxy: Boolean) {
        environment {
            config =
                ApplicationConfig("application.conf").mergeWith(
                    MapApplicationConfig("ptt.trustProxy" to trustProxy.toString()),
                )
        }
    }

    private suspend fun ApplicationTestBuilder.loginFrom(clientIp: String): HttpStatusCode {
        val client =
            createClient {
                install(ContentNegotiation) {
                    json(
                        Json {
                            isLenient = true
                            ignoreUnknownKeys = true
                        },
                    )
                }
            }
        return client
            .post("/api/auth/login") {
                header("X-Forwarded-For", clientIp)
                contentType(ContentType.Application.Json)
                setBody(LoginRequest("TestUser", "device-123"))
            }.status
    }

    @Test
    fun trustingTheProxyGivesEachForwardedClientItsOwnLimit() =
        testApplication {
            withTrustProxy(true)

            repeat(6) { i ->
                assertEquals(HttpStatusCode.OK, loginFrom("10.1.1.$i"), "client 10.1.1.$i should not be limited")
            }
        }

    @Test
    fun withoutTrustingTheProxyForwardedClientsShareTheLimit() =
        testApplication {
            withTrustProxy(false)

            repeat(5) { i ->
                assertEquals(HttpStatusCode.OK, loginFrom("10.1.1.$i"))
            }
            assertEquals(HttpStatusCode.TooManyRequests, loginFrom("10.1.1.6"))
        }
}
