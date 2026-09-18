package com.pttlan.domain.ptt.usecase

import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.domain.ptt.repository.ServerNode
import com.pttlan.domain.ptt.repository.VoiceRepository
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class VoiceAndConnectionUseCasesTest {
    private val voiceRepository: VoiceRepository = mockk(relaxed = true)
    private val connectionRepository: ConnectionRepository = mockk(relaxed = true)

    @Test
    fun `start transmitting asks for the floor`() =
        runTest {
            StartTransmittingUseCase(voiceRepository)("geral", "u1")

            coVerify { voiceRepository.requestFloor("geral", "u1") }
        }

    @Test
    fun `stop transmitting stops the capture before releasing the floor`() =
        runTest {
            StopTransmittingUseCase(voiceRepository)("geral", "u1")

            // Releasing first would let the next speaker in while this device was still sending audio
            coVerifyOrder {
                voiceRepository.stopTransmitting()
                voiceRepository.releaseFloor("geral", "u1")
            }
        }

    @Test
    fun `connection status and discovery come from the repository`() =
        runTest {
            val node = ServerNode("PTT-LAN", ServerEndpoint("192.168.0.50", 9443, true))
            every { connectionRepository.connectionStatus } returns MutableStateFlow(ConnectionStatus.Connected)
            every { connectionRepository.discoverServers() } returns flowOf(node)

            assertEquals(ConnectionStatus.Connected, ObserveConnectionStatusUseCase(connectionRepository)().first())
            assertEquals(node, DiscoverServersUseCase(connectionRepository)().first())
        }
}
