package com.pttlan.server

import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.server.application.Application
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import java.util.UUID

const val PTT_PORT = 9443

private const val KEY_ALIAS = "pttlan"

/**
 * The server running inside a client app (host mode, ADR 0010). Same module as `serverApp`, but with no
 * `application.conf`: every `ptt.*` setting takes its default, so the admin write routes (and the
 * `exitProcess` behind shutdown) stay off. The certificate is generated in memory on each start —
 * clients accept any certificate on the LAN, so there is nothing to persist.
 */
class PttHostServer(
    private val port: Int = PTT_PORT,
    private val announce: (port: Int, serviceName: String) -> AutoCloseable? = ::announceOnLan,
) {
    private var server: EmbeddedServer<*, *>? = null
    private var announcement: AutoCloseable? = null

    val isRunning: Boolean get() = server != null

    /** Starts the server and announces it on the LAN. Does nothing when it is already running. */
    @Synchronized
    fun start(serviceName: String) {
        if (server != null) return

        val password = UUID.randomUUID().toString()
        val keyStore =
            buildKeyStore {
                certificate(KEY_ALIAS) {
                    this.password = password
                    domains = listOf("localhost")
                }
            }
        server =
            embeddedServer(
                Netty,
                configure = {
                    sslConnector(
                        keyStore = keyStore,
                        keyAlias = KEY_ALIAS,
                        keyStorePassword = { password.toCharArray() },
                        privateKeyPassword = { password.toCharArray() },
                    ) {
                        port = this@PttHostServer.port
                    }
                },
                module = Application::module,
            ).also { it.start(wait = false) }
        announcement = announce(port, serviceName)
    }

    @Synchronized
    fun stop() {
        announcement?.close()
        announcement = null
        server?.stop()
        server = null
    }
}
