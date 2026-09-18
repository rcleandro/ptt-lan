package com.pttlan.core.audio

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JitterBufferPolicyTest {
    private val policy = JitterBufferPolicy(prebufferPackets = 3, resyncWindow = 20)

    @Test
    fun waitsUntilEnoughPacketsAreQueued() {
        assertTrue(policy.shouldWaitForMore(queueSize = 0))
        assertTrue(policy.shouldWaitForMore(queueSize = 2))
        assertFalse(policy.shouldWaitForMore(queueSize = 3))
        // Once playing, a momentarily empty queue does not send it back to buffering on its own
        assertFalse(policy.shouldWaitForMore(queueSize = 0))
    }

    @Test
    fun playsPacketsInOrderAndDropsTheLateOnes() {
        assertTrue(policy.shouldPlay(10))
        assertTrue(policy.shouldPlay(11))
        assertFalse(policy.shouldPlay(11), "a repeated packet must not be played twice")
        assertFalse(policy.shouldPlay(10), "a packet that arrives after its turn is dropped")
        assertTrue(policy.shouldPlay(12))
    }

    @Test
    fun acceptsAGapForwardBecauseTheMissingPacketsAreLost() {
        assertTrue(policy.shouldPlay(1))
        assertTrue(policy.shouldPlay(9))
        assertFalse(policy.shouldPlay(5))
    }

    @Test
    fun resyncsWhenTheSequenceRestartsFarBack() {
        assertTrue(policy.shouldPlay(100))
        // A new talk spurt starts its own numbering; without the resync window everything would be dropped
        assertTrue(policy.shouldPlay(0))
        assertTrue(policy.shouldPlay(1))
    }

    @Test
    fun starvationBuffersAgainAndResyncs() {
        assertFalse(policy.shouldWaitForMore(queueSize = 5))
        assertTrue(policy.shouldPlay(50))

        policy.onStarved()

        assertTrue(policy.shouldWaitForMore(queueSize = 1))
        assertTrue(policy.shouldPlay(7), "after starving, the next packet sets the new baseline")
    }
}
