package com.pttlan.domain.ptt.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackPositionTest {
    @Test
    fun `fraction is the share of the message already played`() {
        assertEquals(0.5f, PlaybackPosition("m1", positionMs = 500, durationMs = 1_000).fraction)
        assertEquals(0f, PlaybackPosition("m1", positionMs = 0, durationMs = 1_000).fraction)
        assertEquals(1f, PlaybackPosition("m1", positionMs = 1_000, durationMs = 1_000).fraction)
    }

    @Test
    fun `fraction stays inside zero and one`() {
        // A position past the end (rounding) or a message with no duration must not break the progress bar
        assertEquals(1f, PlaybackPosition("m1", positionMs = 1_200, durationMs = 1_000).fraction)
        assertEquals(0f, PlaybackPosition("m1", positionMs = 500, durationMs = 0).fraction)
    }
}
