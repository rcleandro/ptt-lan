package com.pttlan.domain.ptt.usecase

import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.ServerEndpoint
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectToServerUseCaseTest {
    private val connectionRepository: ConnectionRepository = mockk()
    private val useCase = ConnectToServerUseCase(connectionRepository)

    @Test
    fun `invoke should return failure when nickname is blank`() =
        runTest {
            val endpoint = ServerEndpoint("192.168.0.1", 8080, true)
            val result = useCase(endpoint, "   ")

            assertTrue(result.isFailure)
            assertEquals("Nickname cannot be empty", result.exceptionOrNull()?.message)

            coVerify(exactly = 0) { connectionRepository.connect(any(), any()) }
        }

    @Test
    fun `invoke should call repository connect when nickname is valid`() =
        runTest {
            val host = "192.168.0.1"
            val port = 8080
            val nickname = "User1"

            val endpoint = ServerEndpoint(host, port, true)

            coEvery { connectionRepository.connect(endpoint, nickname) } returns Result.success(Unit)

            val result = useCase(endpoint, nickname)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { connectionRepository.connect(endpoint, nickname) }
        }

    @Test
    fun `invoke should return repository failure when connect fails`() =
        runTest {
            val host = "192.168.0.1"
            val port = 8080
            val nickname = "User1"
            val endpoint = ServerEndpoint(host, port, true)
            val exception = RuntimeException("Connection refused")

            coEvery { connectionRepository.connect(endpoint, nickname) } returns Result.failure(exception)

            val result = useCase(endpoint, nickname)

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            coVerify(exactly = 1) { connectionRepository.connect(endpoint, nickname) }
        }
}
