package com.pttlan.server.auth

import java.security.MessageDigest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * The room PIN, locking the room after [maxFailures] wrong ones in a row (30.3). The login rate limit counts per
 * IP, so an attacker with a few addresses kept guessing; this counts for the room. While locked, even the right
 * PIN is refused; who is already in the room stays.
 */
class PinGuard(
    private val pin: String,
    private val maxFailures: Int = 5,
    private val lockout: Duration = 5.minutes,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    enum class Result { ACCEPTED, REJECTED, LOCKED }

    private var failures = 0
    private var lockedUntilMs = 0L

    @Synchronized
    fun check(attempt: String?): Result =
        when {
            nowMs() < lockedUntilMs -> {
                Result.LOCKED
            }

            // Constant time, so the time to answer does not tell how many characters were right
            MessageDigest.isEqual(attempt.orEmpty().toByteArray(), pin.toByteArray()) -> {
                failures = 0
                Result.ACCEPTED
            }

            ++failures < maxFailures -> {
                Result.REJECTED
            }

            else -> {
                failures = 0
                lockedUntilMs = nowMs() + lockout.inWholeMilliseconds
                Result.LOCKED
            }
        }
}
