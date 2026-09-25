package com.pttlan.domain.ptt.usecase

import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.domain.ptt.repository.ServerNode
import kotlinx.coroutines.flow.Flow

/**
 * UseCase responsável por observar o status de conexão atual com o servidor (Conectado, Desconectado, etc).
 */
class ObserveConnectionStatusUseCase(
    private val connectionRepository: ConnectionRepository,
) {
    operator fun invoke(): Flow<ConnectionStatus> = connectionRepository.connectionStatus
}

/**
 * UseCase responsável por iniciar a descoberta de servidores PTT na rede local usando JmDNS.
 */
class DiscoverServersUseCase(
    private val connectionRepository: ConnectionRepository,
) {
    operator fun invoke(): Flow<List<ServerNode>> = connectionRepository.discoverServers()
}

/**
 * UseCase responsável por conectar ativamente em um servidor PTT descoberto via WebSocket.
 */
class ConnectToServerUseCase(
    private val connectionRepository: ConnectionRepository,
) {
    suspend operator fun invoke(
        endpoint: ServerEndpoint,
        nickname: String,
        pin: String? = null,
    ): Result<Unit> {
        if (nickname.isBlank()) {
            return Result.failure(IllegalArgumentException("Nickname cannot be empty"))
        }
        return connectionRepository.connect(endpoint, nickname, pin)
    }
}

/** Trusts a LAN server's new certificate after it changed and the user compared the codes (30.5). */
class TrustServerCertificateUseCase(
    private val connectionRepository: ConnectionRepository,
) {
    operator fun invoke(endpoint: ServerEndpoint) = connectionRepository.trustServerCertificate(endpoint)
}

/** Code of the connected LAN server's certificate, shown so people can compare it with the host's (30.5). */
class GetServerCertificateCodeUseCase(
    private val connectionRepository: ConnectionRepository,
) {
    operator fun invoke(): String? = connectionRepository.serverCertificateCode
}
