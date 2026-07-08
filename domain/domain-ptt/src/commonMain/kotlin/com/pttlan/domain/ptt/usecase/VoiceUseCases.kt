package com.pttlan.domain.ptt.usecase

import com.pttlan.domain.ptt.repository.VoiceRepository

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
