package com.pttlan.domain.ptt.model

/** Where the replay of [messageId] currently is, so the UI can show a progress bar and the elapsed time. */
data class PlaybackPosition(
    val messageId: String,
    val positionMs: Long,
    val durationMs: Long,
) {
    /** 0f..1f, safe to hand straight to a progress indicator. */
    val fraction: Float
        get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}
