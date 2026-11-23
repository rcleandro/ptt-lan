package com.pttlan.feature.history

import com.pttlan.core.designsystem.UiText
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.channel_name
import com.pttlan.core.designsystem.generated.resources.decimal_separator
import com.pttlan.core.designsystem.generated.resources.history_paused
import com.pttlan.core.designsystem.generated.resources.history_player_subtitle
import com.pttlan.core.designsystem.generated.resources.history_playing
import com.pttlan.core.designsystem.generated.resources.history_queue_position
import com.pttlan.core.designsystem.generated.resources.history_speed
import com.pttlan.core.designsystem.generated.resources.history_status_paused
import com.pttlan.core.designsystem.generated.resources.history_status_playing
import com.pttlan.core.designsystem.generated.resources.history_status_recorded
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.feature.history.util.toRelativeTime
import com.pttlan.feature.history.util.toUiText
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

// What the history screen says, shared by the Compose and SwiftUI screens (32.4): the screens only draw it

private const val MS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val TWO_DIGITS = 2

/** How far the rewind and forward buttons jump. */
const val SEEK_STEP_MS = 5_000L

/** A room in the history: its messages as the list shows them, and how many were not heard yet. */
data class HistoryGroup(
    val channelId: String,
    val messages: List<VoiceMessage>,
    val unheardCount: Int,
)

fun historyGroups(messages: List<VoiceMessage>): List<HistoryGroup> =
    messages.groupBy { it.channelId }.map { (channelId, inRoom) -> HistoryGroup(channelId, inRoom, inRoom.count { it.playedAt == null }) }

/** "0:12": a message's length, rounded up so a short one never reads as zero. */
fun durationText(durationMs: Long): String = positionText((durationMs / MS_PER_SECOND).coerceAtLeast(1) * MS_PER_SECOND)

/** Like [durationText], but an elapsed time of zero shows as `0:00`. */
fun positionText(positionMs: Long): String {
    val totalSeconds = positionMs / MS_PER_SECOND
    val seconds = (totalSeconds % SECONDS_PER_MINUTE).toString().padStart(TWO_DIGITS, '0')
    return "${totalSeconds / SECONDS_PER_MINUTE}:$seconds"
}

/** "tocando · 0:12", "pausado · 0:12", or when it was recorded and how long it is. */
fun messageStatus(
    message: VoiceMessage,
    isPlaying: Boolean,
    isPaused: Boolean,
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): UiText {
    val duration = durationText(message.durationMs)
    return when {
        isPlaying && isPaused -> {
            UiText.Res(Res.string.history_status_paused, listOf(duration))
        }

        isPlaying -> {
            UiText.Res(Res.string.history_status_playing, listOf(duration))
        }

        else -> {
            val recorded = Instant.fromEpochMilliseconds(message.recordedAt).toRelativeTime(now, timeZone)
            UiText.Res(Res.string.history_status_recorded, listOf(recorded.toUiText(), duration))
        }
    }
}

/**
 * Line under the sender in the mini player: the room, and where the queue is ("2 de 5") while a room plays in
 * sequence, or whether it is playing. [queueIndex] is -1 for a single message.
 */
fun playerSubtitle(
    message: VoiceMessage,
    isPaused: Boolean,
    queueIndex: Int,
    queueSize: Int,
): UiText {
    val status =
        when {
            queueIndex >= 0 -> UiText.Res(Res.string.history_queue_position, listOf(queueIndex + 1, queueSize))
            isPaused -> UiText.Res(Res.string.history_paused)
            else -> UiText.Res(Res.string.history_playing)
        }
    return UiText.Res(Res.string.history_player_subtitle, listOf(UiText.Res(Res.string.channel_name, listOf(message.channelId)), status))
}

/** "1×", "1,5×", "2×": whole speeds without decimals, the others with the language's separator. */
fun speedText(speed: Float): UiText {
    val number =
        if (speed % 1f == 0f) {
            speed.toInt().toString()
        } else {
            val (whole, fraction) = speed.toString().split(".")
            UiText.Concat(listOf(whole, UiText.Res(Res.string.decimal_separator), fraction))
        }
    return UiText.Res(Res.string.history_speed, listOf(number))
}
