package com.pttlan.feature.history.util

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** When a message was recorded, as the history shows it: relative while recent, the full date after a day. */
class RelativeTimeTest {
    private val now = Instant.parse("2026-09-25T18:00:00Z")

    private fun ago(duration: Duration) = (now - duration).toRelativeTime(now, TimeZone.UTC)

    @Test
    fun `under a second is just now`() {
        assertEquals(RelativeTime.JustNow, ago(500.milliseconds))
    }

    @Test
    fun `seconds, minutes and hours ago while recent`() {
        assertEquals(RelativeTime.Seconds(42), ago(42.seconds))
        assertEquals(RelativeTime.Minutes(5), ago(5.minutes))
        assertEquals(RelativeTime.Hours(2, 15), ago(2.hours + 15.minutes))
        assertEquals(RelativeTime.Hours(3, 0), ago(3.hours))
    }

    @Test
    fun `a day or more, or a time in the future, shows the full date`() {
        assertEquals(RelativeTime.FullDate(24, 9, 2026, 17, 0), ago(25.hours))
        assertEquals(RelativeTime.FullDate(25, 9, 2026, 18, 1), ago((-1).minutes))
    }
}
