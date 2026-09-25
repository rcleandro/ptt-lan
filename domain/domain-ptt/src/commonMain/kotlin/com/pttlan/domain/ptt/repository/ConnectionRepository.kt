package com.pttlan.domain.ptt.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class ConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Reconnecting,
}

data class ServerEndpoint(
    val host: String,
    val port: Int,
    val isLocal: Boolean,
)

data class ServerNode(
    val name: String,
    val endpoint: ServerEndpoint,
)

interface ConnectionRepository {
    val connectionStatus: StateFlow<ConnectionStatus>

    /**
     * Identity issued by the server at login (JWT `sub`). The server ignores ids sent in messages and uses
     * this one, so it is the only id that matches `SpeakerChanged`/`ParticipantList` for the local user.
     * Null while not logged in.
     */
    val sessionUserId: String?

    /** Reason the server gave for closing the session ("Nome já em uso", …), when there was one. */
    val lastDisconnectReason: String?

    /**
     * Short code of the certificate the connected LAN server showed, to compare with the one its host sees (30.5).
     * Null off the LAN, where the system validates the certificate.
     */
    val serverCertificateCode: String?

    /** Trusts the new certificate [endpoint] showed after it changed, once the user compared the codes (30.5). */
    fun trustServerCertificate(endpoint: ServerEndpoint)

    /** The servers on the network right now, re-emitted whenever one appears or leaves. */
    fun discoverServers(): Flow<List<ServerNode>>

    fun stopDiscovery()

    /** [pin] is the room PIN of a hosted channel (host mode); null for servers without one. */
    suspend fun connect(
        endpoint: ServerEndpoint,
        nickname: String,
        pin: String? = null,
    ): Result<Unit>

    fun disconnect()
}
