package com.pttlan.domain.ptt.usecase

import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerNode
import kotlinx.coroutines.flow.Flow

class ObserveConnectionStatusUseCase(
    private val connectionRepository: ConnectionRepository,
) {
    operator fun invoke(): Flow<ConnectionStatus> = connectionRepository.connectionStatus
}

class DiscoverServersUseCase(
    private val connectionRepository: ConnectionRepository,
) {
    operator fun invoke(): Flow<ServerNode> = connectionRepository.discoverServers()
}

class ConnectToServerUseCase(
    private val connectionRepository: ConnectionRepository,
) {
    suspend operator fun invoke(
        host: String,
        port: Int,
        nickname: String,
    ): Result<Unit> {
        if (nickname.isBlank()) {
            return Result.failure(IllegalArgumentException("Nickname cannot be empty"))
        }
        return connectionRepository.connect(host, port, nickname)
    }
}
