package com.pttlan.domain.ptt.repository

/** The live audio path: who holds the floor and the transmission itself. History lives in [HistoryRepository]. */
interface VoiceRepository {
    suspend fun requestFloor(
        channelId: String,
        userId: String,
    )

    suspend fun releaseFloor(
        channelId: String,
        userId: String,
    )

    suspend fun startTransmitting(
        channelId: String,
        userId: String,
    )

    suspend fun stopTransmitting()
}
