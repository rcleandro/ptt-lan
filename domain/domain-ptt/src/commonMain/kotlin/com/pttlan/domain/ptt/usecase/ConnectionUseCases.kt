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
    operator fun invoke(): Flow<ServerNode> = connectionRepository.discoverServers()
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
    ): Result<Unit> {
        if (nickname.isBlank()) {
            return Result.failure(IllegalArgumentException("Nickname cannot be empty"))
        }
        return connectionRepository.connect(endpoint, nickname)
    }
}
