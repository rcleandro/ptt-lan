package com.pttlan.domain.ptt.usecase

import com.pttlan.domain.ptt.model.ParticipantDomain
import com.pttlan.domain.ptt.repository.ActiveChannelDomain
import com.pttlan.domain.ptt.repository.ChannelDomain
import com.pttlan.domain.ptt.repository.ChannelRepository
import com.pttlan.domain.ptt.repository.ChannelSessionRepository
import com.pttlan.domain.ptt.repository.SpeakerState
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChannelUseCasesTest {
    private val channelRepository: ChannelRepository = mockk(relaxed = true)
    private val sessionRepository: ChannelSessionRepository = mockk(relaxed = true)

    @Test
    fun `create channel turns the name into a slug and saves it`() =
        runTest {
            val id = CreateChannelUseCase(channelRepository)("Obra Alameda")

            assertEquals("obra-alameda", id)
            coVerify { channelRepository.saveChannel(ChannelDomain("obra-alameda", "Obra Alameda", false)) }
        }

    @Test
    fun `create channel refuses a blank name`() =
        runTest {
            assertFailsWith<IllegalArgumentException> { CreateChannelUseCase(channelRepository)("   ") }

            coVerify(exactly = 0) { channelRepository.saveChannel(any()) }
        }

    @Test
    fun `joining a channel records it in the recent list`() =
        runTest {
            JoinChannelUseCaseImpl(channelRepository)("geral", "Geral")

            coVerify { channelRepository.saveChannel(ChannelDomain("geral", "Geral", false)) }
        }

    @Test
    fun `recent and active channels come from the repository`() =
        runTest {
            val recent = listOf(ChannelDomain("geral", "Geral", false))
            val active = listOf(ActiveChannelDomain("geral", 3))
            every { channelRepository.getRecentChannels() } returns flowOf(recent)
            every { channelRepository.observeActiveChannels() } returns flowOf(active)

            assertEquals(recent, GetRecentChannelsUseCase(channelRepository)().first())
            assertEquals(active, ObserveActiveChannelsUseCase(channelRepository)().first())
        }

    @Test
    fun `join and leave reach the session repository with the caller identity`() =
        runTest {
            JoinChannelUseCase(sessionRepository)("geral", "u1", "Tester")
            LeaveChannelUseCase(sessionRepository)("geral", "u1")

            coVerify { sessionRepository.joinChannel("geral", "u1", "Tester") }
            coVerify { sessionRepository.leaveChannel("geral", "u1") }
        }

    @Test
    fun `the observers forward what the session repository emits`() =
        runTest {
            val participants = listOf(ParticipantDomain("u1", "Tester", false))
            val speaker = SpeakerState("u1", "Tester", true)
            every { sessionRepository.observeParticipants("geral") } returns flowOf(participants)
            every { sessionRepository.observeSpeaker("geral") } returns flowOf(speaker)
            every { sessionRepository.observeFloorDenied("geral") } returns flowOf("Alguém já está falando")

            assertEquals(participants, ObserveParticipantsUseCase(sessionRepository)("geral").first())
            assertEquals(speaker, ObserveSpeakerUseCase(sessionRepository)("geral").first())
            assertEquals("Alguém já está falando", ObserveFloorDeniedUseCase(sessionRepository)("geral").first())
        }
}
