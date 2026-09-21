package com.pttlan.data.ptt.repository

import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.protocol.LoginResponse
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.russhwolf.settings.MapSettings
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReconnectOverSessionTest {
    private val endpoint = ServerEndpoint("localhost", 9443, isLocal = true)

    @Test
    fun connectingAgainWhileConnectedEndsConnected() =
        runTest {
            // Going back to the connection screen keeps the session; hosting again connects over it. The old
            // session's teardown used to mark the new one Disconnected, and the app fell back to the home screen.
            val isConnected = MutableStateFlow(false)
            val client: PttWebSocketClient = mockk(relaxed = true)
            every { client.isConnected } returns isConnected
            coEvery { client.login(any(), any(), any(), any(), any(), any()) } returns LoginResponse("token", "user")
            coEvery { client.connect(any(), any(), any(), any()) } coAnswers {
                isConnected.value = true
                try {
                    awaitCancellation()
                } finally {
                    isConnected.value = false
                }
            }
            val repository = ConnectionRepositoryImpl(mockk(), client, MapSettings(), StandardTestDispatcher(testScheduler))

            assertTrue(repository.connect(endpoint, "Host", null).isSuccess)
            val seen = mutableListOf<ConnectionStatus>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.connectionStatus.toList(seen) }

            assertTrue(repository.connect(endpoint, "Host", null).isSuccess)
            advanceUntilIdle()

            // RootComponent sends the user home on any Disconnected, so not even a transient one may show up.
            assertFalse(ConnectionStatus.Disconnected in seen, "statuses seen: $seen")
            assertEquals(ConnectionStatus.Connected, repository.connectionStatus.value)
        }

    @Test
    fun twoConnectsAtOnceLeaveASingleConnectionLoop() =
        runTest {
            // A double tap on "Hospedar" with a session open: both calls waited on the same old job and each
            // started its own loop, so two loops shared one client.
            val isConnected = MutableStateFlow(false)
            var runningLoops = 0
            val client: PttWebSocketClient = mockk(relaxed = true)
            every { client.isConnected } returns isConnected
            coEvery { client.login(any(), any(), any(), any(), any(), any()) } returns LoginResponse("token", "user")
            coEvery { client.connect(any(), any(), any(), any()) } coAnswers {
                runningLoops++
                isConnected.value = true
                try {
                    awaitCancellation()
                } finally {
                    runningLoops--
                }
            }
            val repository = ConnectionRepositoryImpl(mockk(), client, MapSettings(), StandardTestDispatcher(testScheduler))
            repository.connect(endpoint, "Host", null)

            val taps = List(2) { async { repository.connect(endpoint, "Host", null) } }
            taps.awaitAll()
            advanceUntilIdle()

            assertEquals(1, runningLoops)
        }
}
