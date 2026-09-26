package com.pttlan.feature.history.util

import androidx.compose.runtime.Composable
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.time_ago
import com.pttlan.core.designsystem.generated.resources.time_ago_hours_minutes
import com.pttlan.core.designsystem.generated.resources.time_full_date
import com.pttlan.core.designsystem.generated.resources.time_hours
import com.pttlan.core.designsystem.generated.resources.time_just_now
import com.pttlan.core.designsystem.generated.resources.time_minutes
import com.pttlan.core.designsystem.generated.resources.time_seconds
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Past this, a message shows its full date instead of "há …". */
private val RECENT_THRESHOLD = 24.hours

/** Width of a zero-padded day, month, hour or minute. */
private const val TWO_DIGITS = 2

/** When something happened, relative while recent ("há 5 minutos") and as a full date after that. */
sealed interface RelativeTime {
    data object JustNow : RelativeTime

    data class Seconds(
        val seconds: Long,
    ) : RelativeTime

    data class Minutes(
        val minutes: Long,
    ) : RelativeTime

    data class Hours(
        val hours: Long,
        val minutes: Long,
    ) : RelativeTime

    data class FullDate(
        val day: Int,
        val month: Int,
        val year: Int,
        val hour: Int,
        val minute: Int,
    ) : RelativeTime
}

/**
 * How long ago this was, relative while under [recentThreshold] and as a full date after it or when in the future.
 * Only the choice lives here; the text comes from the resources, in [relativeTimeText].
 */
fun Instant.toRelativeTime(
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    recentThreshold: Duration = RECENT_THRESHOLD,
): RelativeTime {
    val duration = now - this
    return when {
        duration.isNegative() || duration >= recentThreshold -> fullDate(timeZone)
        duration < 1.minutes -> if (duration.inWholeSeconds < 1) RelativeTime.JustNow else RelativeTime.Seconds(duration.inWholeSeconds)
        duration < 1.hours -> RelativeTime.Minutes(duration.inWholeMinutes)
        else -> RelativeTime.Hours(duration.inWholeHours, (duration - duration.inWholeHours.hours).inWholeMinutes)
    }
}

private fun Instant.fullDate(timeZone: TimeZone): RelativeTime.FullDate {
    val dt = toLocalDateTime(timeZone)
    return RelativeTime.FullDate(dt.day, dt.month.number, dt.year, dt.hour, dt.minute)
}

/** The text of a [RelativeTime], from the resources: "agora mesmo", "há 2 horas e 15 minutos", "16/07/2024 às 14:41". */
@Composable
fun relativeTimeText(time: RelativeTime): String =
    when (time) {
        RelativeTime.JustNow -> {
            stringResource(Res.string.time_just_now)
        }

        is RelativeTime.Seconds -> {
            stringResource(Res.string.time_ago, pluralStringResource(Res.plurals.time_seconds, time.seconds.toInt(), time.seconds))
        }

        is RelativeTime.Minutes -> {
            stringResource(Res.string.time_ago, pluralStringResource(Res.plurals.time_minutes, time.minutes.toInt(), time.minutes))
        }

        is RelativeTime.Hours -> {
            val hours = pluralStringResource(Res.plurals.time_hours, time.hours.toInt(), time.hours)
            if (time.minutes == 0L) {
                stringResource(Res.string.time_ago, hours)
            } else {
                stringResource(
                    Res.string.time_ago_hours_minutes,
                    hours,
                    pluralStringResource(Res.plurals.time_minutes, time.minutes.toInt(), time.minutes),
                )
            }
        }

        is RelativeTime.FullDate -> {
            stringResource(
                Res.string.time_full_date,
                time.day.twoDigits(),
                time.month.twoDigits(),
                time.year,
                time.hour.twoDigits(),
                time.minute.twoDigits(),
            )
        }
    }

private fun Int.twoDigits() = toString().padStart(TWO_DIGITS, '0')
