package com.pttlan.server.channel

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** 30.10: anyone in an open room could flood everybody's channel list with new names. */
class ChannelLimitsTest {
    @Test
    fun `a channel name longer than 40 characters is refused`() =
        runTest {
            val registry = ChannelRegistry(dispatcher = StandardTestDispatcher(testScheduler))

            assertNotNull(registry.refusalToOpen("x".repeat(41)))
            assertNull(registry.refusalToOpen("x".repeat(40)))
        }

    @Test
    fun `past 50 channels a new one is refused, but the ones there can still be joined`() =
        runTest {
            val registry = ChannelRegistry(dispatcher = StandardTestDispatcher(testScheduler))
            repeat(50) { registry.getOrCreateChannel("canal-$it") }

            assertNotNull(registry.refusalToOpen("mais-um"))
            assertNull(registry.refusalToOpen("canal-3"))
        }
}
