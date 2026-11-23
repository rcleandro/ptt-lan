package com.pttlan.feature.history

import com.pttlan.core.designsystem.UiText
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.channel_name
import com.pttlan.core.designsystem.generated.resources.decimal_separator
import com.pttlan.core.designsystem.generated.resources.history_paused
import com.pttlan.core.designsystem.generated.resources.history_player_subtitle
import com.pttlan.core.designsystem.generated.resources.history_queue_position
import com.pttlan.core.designsystem.generated.resources.history_speed
import com.pttlan.core.designsystem.generated.resources.history_status_paused
import com.pttlan.core.designsystem.generated.resources.history_status_playing
import com.pttlan.core.designsystem.generated.resources.history_status_recorded
import com.pttlan.core.designsystem.generated.resources.time_ago
import com.pttlan.core.designsystem.generated.resources.time_minutes
import com.pttlan.domain.ptt.model.VoiceMessage
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** What the Compose and SwiftUI history screens both show (32.4). */
class HistoryTextTest {
    private val now = Instant.fromEpochMilliseconds(1_800_000_000_000)

    private fun message(
        id: String,
        channelId: String = "Geral",
        playedAt: Long? = null,
        durationMs: Long = 12_000,
        recordedAt: Long = (now - 5.minutes).toEpochMilliseconds(),
    ) = VoiceMessage(id, channelId, "Marcos", "/$id", durationMs, recordedAt, playedAt)

    @Test
    fun messagesAreGroupedByRoomWithTheUnheardCounted() {
        val groups = historyGroups(listOf(message("1"), message("2", playedAt = 1), message("3", channelId = "Obra")))
        assertEquals(listOf("Geral", "Obra"), groups.map { it.channelId })
        assertEquals(listOf(1, 1), groups.map { it.unheardCount })
        assertEquals(listOf("1", "2"), groups.first().messages.map { it.id })
    }

    @Test
    fun aLengthIsRoundedUpButAnElapsedZeroIsNot() {
        assertEquals("0:01", durationText(300))
        assertEquals("1:05", durationText(65_000))
        assertEquals("0:00", positionText(0))
        assertEquals("0:07", positionText(7_900))
    }

    @Test
    fun aMessageSaysWhetherItPlaysOrWhenItWasRecorded() {
        val m = message("1")
        assertEquals(
            UiText.Res(Res.string.history_status_playing, listOf("0:12")),
            messageStatus(m, isPlaying = true, isPaused = false, now),
        )
        assertEquals(UiText.Res(Res.string.history_status_paused, listOf("0:12")), messageStatus(m, isPlaying = true, isPaused = true, now))
        val fiveMinutesAgo = UiText.Res(Res.string.time_ago, listOf(UiText.Plural(Res.plurals.time_minutes, 5, listOf(5L))))
        assertEquals(
            UiText.Res(Res.string.history_status_recorded, listOf(fiveMinutesAgo, "0:12")),
            messageStatus(m, isPlaying = false, isPaused = false, now, TimeZone.UTC),
        )
    }

    @Test
    fun theMiniPlayerShowsTheQueuePositionWhileARoomPlays() {
        val room = UiText.Res(Res.string.channel_name, listOf("Geral"))
        assertEquals(
            UiText.Res(Res.string.history_player_subtitle, listOf(room, UiText.Res(Res.string.history_queue_position, listOf(2, 5)))),
            playerSubtitle(message("1"), isPaused = false, queueIndex = 1, queueSize = 5),
        )
        assertEquals(
            UiText.Res(Res.string.history_player_subtitle, listOf(room, UiText.Res(Res.string.history_paused))),
            playerSubtitle(message("1"), isPaused = true, queueIndex = -1, queueSize = 0),
        )
    }

    @Test
    fun wholeSpeedsHaveNoDecimalsAndTheOthersUseTheLanguagesSeparator() {
        assertEquals(UiText.Res(Res.string.history_speed, listOf("2")), speedText(2f))
        assertEquals(
            UiText.Res(Res.string.history_speed, listOf(UiText.Concat(listOf("1", UiText.Res(Res.string.decimal_separator), "5")))),
            speedText(1.5f),
        )
    }
}
