package com.pttlan.domain.ptt.usecase

import com.pttlan.domain.ptt.repository.VoiceRepository

/**
 * UseCase responsável por solicitar o direito de fala (floor control) e iniciar a captura de áudio.
 */
class StartTransmittingUseCase(
    private val repo: VoiceRepository,
) {
    suspend operator fun invoke(
        channelId: String,
        userId: String,
    ) {
        repo.requestFloor(channelId, userId)
    }
}

/**
 * UseCase responsável por parar a captura de áudio e liberar o direito de fala.
 */
class StopTransmittingUseCase(
    private val repo: VoiceRepository,
) {
    suspend operator fun invoke(
        channelId: String,
        userId: String,
    ) {
        repo.stopTransmitting()
        repo.releaseFloor(channelId, userId)
    }
}
