package com.pttlan.server.channel

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

/**
 * Empty channels are removed five minutes after the last participant leaves. Virtual time keeps the test
 * honest about the delay without waiting for it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChannelRegistryCleanupTest {
    @Test
    fun removesAnEmptyChannelAfterTheCleanupDelay() =
        runTest {
            val registry = ChannelRegistry(dispatcher = StandardTestDispatcher(testScheduler))
            registry.getOrCreateChannel("obra")

            registry.scheduleCleanupIfEmpty("obra")

            advanceTimeBy(4.minutes)
            runCurrent()
            assertNotNull(registry.getChannel("obra"), "the channel is still there before the delay")

            advanceTimeBy(2.minutes)
            runCurrent()
            assertNull(registry.getChannel("obra"), "an empty channel is dropped after five minutes")
        }

    @Test
    fun keepsAChannelSomebodyJoinedInTheMeantime() =
        runTest {
            val registry = ChannelRegistry(dispatcher = StandardTestDispatcher(testScheduler))
            val channel = registry.getOrCreateChannel("obra")

            registry.scheduleCleanupIfEmpty("obra")
            advanceTimeBy(1.minutes)
            runCurrent()
            channel.addParticipant(Participant("u1", "Tester", mockk(relaxed = true)))

            advanceTimeBy(10.minutes)
            runCurrent()

            assertNotNull(registry.getChannel("obra"), "a channel with people in it must survive the cleanup")
        }

    @Test
    fun neverRemovesTheDefaultChannel() =
        runTest {
            val registry = ChannelRegistry(dispatcher = StandardTestDispatcher(testScheduler))

            registry.scheduleCleanupIfEmpty("Geral")
            advanceTimeBy(10.minutes)
            runCurrent()

            assertNotNull(registry.getChannel("Geral"), "Geral always exists")
            assertEquals(0, registry.getGlobalConnectionsCount())
        }
}
