package com.pttlan.server.auth

import com.pttlan.server.auth.PinGuard.Result.ACCEPTED
import com.pttlan.server.auth.PinGuard.Result.LOCKED
import com.pttlan.server.auth.PinGuard.Result.REJECTED
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

/** 30.3: a short PIN fell to brute force in minutes; the room now locks after a run of wrong ones. */
class PinGuardTest {
    private var nowMs = 0L
    private val guard = PinGuard("482193", maxFailures = 5, lockout = 5.minutes) { nowMs }

    @Test
    fun `the right pin gets in and a wrong one does not`() {
        assertEquals(ACCEPTED, guard.check("482193"))
        assertEquals(REJECTED, guard.check("000000"))
        assertEquals(REJECTED, guard.check(null))
    }

    @Test
    fun `five wrong pins in a row lock the room, even for the right pin, until the lockout ends`() {
        repeat(4) { assertEquals(REJECTED, guard.check("000000")) }
        assertEquals(LOCKED, guard.check("000000"), "the fifth miss locks, and says so")

        assertEquals(LOCKED, guard.check("482193"))
        nowMs += 5.minutes.inWholeMilliseconds - 1
        assertEquals(LOCKED, guard.check("482193"))
        nowMs += 1
        assertEquals(ACCEPTED, guard.check("482193"))
    }

    @Test
    fun `getting it right starts the count over`() {
        repeat(4) { guard.check("000000") }
        assertEquals(ACCEPTED, guard.check("482193"))

        repeat(4) { assertEquals(REJECTED, guard.check("000000")) }
    }
}
