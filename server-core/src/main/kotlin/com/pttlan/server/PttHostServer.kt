package com.pttlan.server

import com.pttlan.server.auth.JwtConfig
import com.pttlan.server.channel.ChannelRegistry
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.server.application.Application
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.getKoin
import java.util.UUID

const val PTT_PORT = 9443

private const val KEY_ALIAS = "pttlan"

/** Shortest room PIN host mode accepts: 6 digits take years to guess at the lockout's pace (30.3). */
const val MIN_PIN_LENGTH = 6

/**
 * The server running inside a client app (host mode, ADR 0010). Same module as `serverApp`, but with no
 * `application.conf` and no admin panel: the panel's metrics need JVM management beans that Android lacks, and its
 * shutdown would `exitProcess` the app that hosts the room. The certificate is generated in memory on each start —
 * clients accept any certificate on the LAN, so there is nothing to persist.
 */
class PttHostServer(
    private val port: Int = PTT_PORT,
    private val announce: (port: Int, serviceName: String) -> AutoCloseable? = ::announceOnLan,
) {
    private var server: EmbeddedServer<*, *>? = null
    private var announcement: AutoCloseable? = null

    val isRunning: Boolean get() = server != null

    /**
     * Starts the server and announces it on the LAN. Does nothing when it is already running — the PIN of
     * the running room stays. A blank [pin] makes an open room; a shorter one than [MIN_PIN_LENGTH] is refused,
     * because it falls to guessing (30.3).
     */
    @Synchronized
    fun start(
        serviceName: String,
        pin: String? = null,
    ) {
        if (server != null) return
        require(pin.isNullOrBlank() || pin.length >= MIN_PIN_LENGTH) { "O PIN precisa ter pelo menos $MIN_PIN_LENGTH caracteres" }
        // Tokens of an earlier room, with another PIN or none, must not open this one
        JwtConfig.rotateKey()

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
                applicationEnvironment {
                    config =
                        MapApplicationConfig(
                            "ptt.roomPin" to pin.orEmpty(),
                            "ptt.adminPanel" to "false",
                        )
                },
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
        // Tells the others the room ended while their sockets are still open; otherwise they sat on "reconectando"
        // for minutes. serverApp never does this, so its restarts keep clients retrying.
        server?.application?.let { app ->
            runBlocking { app.getKoin().get<ChannelRegistry>().closeRoom("O host encerrou a sala") }
        }
        server?.stop()
        server = null
    }
}
