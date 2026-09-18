package com.pttlan.server

import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

private const val ADMIN_PASSWORD = "s3cret"

class AdminAuthIntegrationTest {
    // application.conf already lists the server module, so testApplication loads it from the config below.
    private fun ApplicationTestBuilder.withAdminPassword(password: String?) {
        environment {
            config =
                ApplicationConfig("application.conf").mergeWith(
                    MapApplicationConfig("ptt.adminPassword" to password.orEmpty()),
                )
        }
    }

    @Test
    fun adminRoutesRequireCredentialsWhenPasswordIsSet() =
        testApplication {
            withAdminPassword(ADMIN_PASSWORD)

            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/admin/metrics").status)
            assertEquals(HttpStatusCode.Unauthorized, client.post("/api/admin/system/restart").status)
            assertEquals(
                HttpStatusCode.Unauthorized,
                client.post("/api/admin/system/restart") { basicAuth("admin", "wrong") }.status,
            )

            assertEquals(
                HttpStatusCode.OK,
                client.get("/api/admin/metrics") { basicAuth("admin", ADMIN_PASSWORD) }.status,
            )
            assertEquals(
                HttpStatusCode.OK,
                client.post("/api/admin/system/restart") { basicAuth("admin", ADMIN_PASSWORD) }.status,
            )
        }

    @Test
    fun writeRoutesAreDisabledWithoutPassword() =
        testApplication {
            withAdminPassword(null)

            assertEquals(HttpStatusCode.OK, client.get("/api/admin/metrics").status)
            assertEquals(HttpStatusCode.NotFound, client.post("/api/admin/system/restart").status)
            assertEquals(HttpStatusCode.NotFound, client.post("/api/admin/system/shutdown").status)
        }
}
