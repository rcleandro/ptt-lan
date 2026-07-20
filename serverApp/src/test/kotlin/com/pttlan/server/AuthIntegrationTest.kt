package com.pttlan.server

import com.pttlan.server.routing.LoginRequest
import com.pttlan.server.routing.LoginResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthIntegrationTest {
    @Test
    fun testLoginSuccessAndRateLimiting() =
        testApplication {
            application {
                module()
            }

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

            val loginReq = LoginRequest("TestUser", "device-123")

            // Deve funcionar na primeira vez
            val response1 =
                client.post("/api/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(loginReq)
                }
            assertEquals(HttpStatusCode.OK, response1.status)
            val body = response1.body<LoginResponse>()
            assertTrue(body.token.isNotBlank())

            // Fazer mais 5 requisições rápidas para acionar o rate limit (o limite é 5)
            for (i in 1..4) {
                client.post("/api/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(loginReq)
                }
            }

            // A 6ª ou 7ª (dependendo de como o RateLimit processa) requisição
            // deve ser bloqueada com 429 Too Many Requests
            val responseBlocked =
                client.post("/api/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(loginReq)
                }
            assertEquals(HttpStatusCode.TooManyRequests, responseBlocked.status)
        }
}
