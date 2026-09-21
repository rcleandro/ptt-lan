package com.pttlan.feature.connection

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.pttlan.core.datastore.SettingsKeys
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.LocalServerHost
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.domain.ptt.repository.ServerNode
import com.pttlan.domain.ptt.usecase.ConnectToServerUseCase
import com.pttlan.domain.ptt.usecase.DiscoverServersUseCase
import com.pttlan.domain.ptt.usecase.ObserveConnectionStatusUseCase
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionComponentTest {
    private val observeConnectionStatusUseCase: ObserveConnectionStatusUseCase = mockk()
    private val discoverServersUseCase: DiscoverServersUseCase = mockk()
    private val connectToServerUseCase: ConnectToServerUseCase = mockk()
    private lateinit var settings: Settings
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settings = MapSettings()

        startKoin {
            modules(
                module {
                    single<Settings> { settings }
                },
            )
        }

        every { observeConnectionStatusUseCase() } returns emptyFlow()
        every { discoverServersUseCase() } returns emptyFlow()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    private fun createComponent(localServerHost: LocalServerHost? = null): ConnectionComponent {
        val lifecycle = LifecycleRegistry()
        return ConnectionComponent(
            componentContext = DefaultComponentContext(lifecycle),
            observeConnectionStatusUseCase = observeConnectionStatusUseCase,
            discoverServersUseCase = discoverServersUseCase,
            connectToServerUseCase = connectToServerUseCase,
            localServerHost = localServerHost,
        )
    }

    @Test
    fun `when ConnectToManualIp with valid nickname and IP, should call use case with correct isLocal flag`() =
        runTest {
            val component = createComponent()

            // Arrange
            component.onIntent(ConnectionIntent.UpdateNickname("User1"))
            component.onIntent(ConnectionIntent.UpdateManualIp("192.168.0.50"))

            val expectedEndpoint = ServerEndpoint("192.168.0.50", 9443, isLocal = true)
            coEvery { connectToServerUseCase(expectedEndpoint, "User1") } returns Result.success(Unit)

            // Act
            component.onIntent(ConnectionIntent.ConnectToManualIp("192.168.0.50"))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { connectToServerUseCase(expectedEndpoint, "User1") }
        }

    @Test
    fun `when ConnectToManualIp with internet domain, should call use case with isLocal false`() =
        runTest {
            val component = createComponent()

            // Arrange
            component.onIntent(ConnectionIntent.UpdateNickname("User2"))
            component.onIntent(ConnectionIntent.UpdateManualIp("ptt.internet.com"))

            val expectedEndpoint = ServerEndpoint("ptt.internet.com", 9443, isLocal = false)
            coEvery { connectToServerUseCase(expectedEndpoint, "User2") } returns Result.success(Unit)

            // Act
            component.onIntent(ConnectionIntent.ConnectToManualIp("ptt.internet.com"))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { connectToServerUseCase(expectedEndpoint, "User2") }
        }

    @Test
    fun `when ConnectToManualIp throws TimeoutCancellationException, should emit ShowError effect`() =
        runTest {
            val component = createComponent()

            // Arrange
            component.onIntent(ConnectionIntent.UpdateNickname("User1"))
            component.onIntent(ConnectionIntent.UpdateManualIp("10.0.0.1"))

            val expectedEndpoint = ServerEndpoint("10.0.0.1", 9443, isLocal = true)
            coEvery { connectToServerUseCase(expectedEndpoint, "User1") } coAnswers {
                Result.failure(
                    runCatching {
                        withTimeout(1.milliseconds) { delay(10.milliseconds) }
                    }.exceptionOrNull() ?: Exception("Timeout"),
                )
            }

            // Act
            component.onIntent(ConnectionIntent.ConnectToManualIp("10.0.0.1"))
            advanceUntilIdle()

            // Assert
            val effect = component.effects.first()
            assertTrue(effect is ConnectionEffect.ShowError)
            assertTrue(effect.message.contains("Tempo de conexão excedido"))
        }

    @Test
    fun `refreshing starts the search over and drops hosts that left`() =
        runTest {
            val gone = ServerNode("PTT-LAN-Gone", ServerEndpoint("192.168.0.20", 9443, isLocal = true))
            val fresh = ServerNode("PTT-LAN-Fresh", ServerEndpoint("192.168.0.21", 9443, isLocal = true))
            every { discoverServersUseCase() } returnsMany listOf(flowOf(gone), flowOf(fresh))
            val component = createComponent()
            advanceUntilIdle()
            assertEquals(listOf(gone), component.state.value.discoveredServers)

            component.onIntent(ConnectionIntent.RefreshServers)
            advanceUntilIdle()

            assertEquals(listOf(fresh), component.state.value.discoveredServers)
        }

    @Test
    fun `destroying the screen stops its network search`() =
        runTest {
            // Leaving the app with back destroys the component while a hosted room keeps the process alive;
            // its NSD search kept running in the background and was never stopped.
            var searching = false
            every { discoverServersUseCase() } returns
                flow<ServerNode> { awaitCancellation() }
                    .onStart { searching = true }
                    .onCompletion { searching = false }
            val lifecycle = LifecycleRegistry()
            lifecycle.resume()
            ConnectionComponent(
                componentContext = DefaultComponentContext(lifecycle),
                observeConnectionStatusUseCase = observeConnectionStatusUseCase,
                discoverServersUseCase = discoverServersUseCase,
                connectToServerUseCase = connectToServerUseCase,
            )
            advanceUntilIdle()
            assertTrue(searching)

            lifecycle.destroy()
            advanceUntilIdle()

            assertFalse(searching)
        }

    @Test
    fun `without a local server host, hosting is not offered`() {
        assertFalse(createComponent().state.value.canHost)
    }

    @Test
    fun `when HostServer succeeds, should connect to the endpoint the host returned`() =
        runTest {
            val host: LocalServerHost = mockk()
            val hostEndpoint = ServerEndpoint("localhost", 9443, isLocal = true)
            coEvery { host.start("PTT-LAN-User1", null) } returns Result.success(hostEndpoint)
            coEvery { connectToServerUseCase(hostEndpoint, "User1") } returns Result.success(Unit)
            val component = createComponent(host)
            assertTrue(component.state.value.canHost)

            component.onIntent(ConnectionIntent.UpdateNickname("User1"))
            component.onIntent(ConnectionIntent.HostServer)
            advanceUntilIdle()

            coVerify(exactly = 1) { connectToServerUseCase(hostEndpoint, "User1") }
        }

    @Test
    fun `a nickname with surrounding spaces hosts and joins trimmed`() =
        runTest {
            // Desktop discovery (JmDNS) never resolves a service whose name ends in a space.
            val host: LocalServerHost = mockk()
            val hostEndpoint = ServerEndpoint("localhost", 9443, isLocal = true)
            coEvery { host.start("PTT-LAN-User1", null) } returns Result.success(hostEndpoint)
            coEvery { connectToServerUseCase(hostEndpoint, "User1") } returns Result.success(Unit)
            val component = createComponent(host)

            component.onIntent(ConnectionIntent.UpdateNickname(" User1 "))
            component.onIntent(ConnectionIntent.HostServer)
            advanceUntilIdle()

            coVerify(exactly = 1) { connectToServerUseCase(hostEndpoint, "User1") }
            assertEquals("User1", settings.getString(SettingsKeys.NICKNAME, ""))
        }

    @Test
    fun `when HostServer fails to start, should emit ShowError and not connect`() =
        runTest {
            val host: LocalServerHost = mockk()
            coEvery { host.start(any(), any()) } returns Result.failure(IllegalStateException("Address already in use"))
            val component = createComponent(host)

            component.onIntent(ConnectionIntent.UpdateNickname("User1"))
            component.onIntent(ConnectionIntent.HostServer)
            advanceUntilIdle()

            val effect = component.effects.first()
            assertTrue(effect is ConnectionEffect.ShowError)
            assertTrue(effect.message.contains("Address already in use"))
            coVerify(exactly = 0) { connectToServerUseCase(any(), any()) }
        }

    @Test
    fun `with a pin, hosting closes the room with it and joins with it`() =
        runTest {
            val host: LocalServerHost = mockk()
            val hostEndpoint = ServerEndpoint("localhost", 9443, isLocal = true)
            coEvery { host.start("PTT-LAN-User1", "4821") } returns Result.success(hostEndpoint)
            coEvery { connectToServerUseCase(hostEndpoint, "User1", "4821") } returns Result.success(Unit)
            val component = createComponent(host)

            component.onIntent(ConnectionIntent.UpdateNickname("User1"))
            component.onIntent(ConnectionIntent.UpdatePin(" 4821 "))
            component.onIntent(ConnectionIntent.HostServer)
            advanceUntilIdle()

            coVerify(exactly = 1) { connectToServerUseCase(hostEndpoint, "User1", "4821") }
        }

    @Test
    fun `joining a server sends the pin typed on the screen`() =
        runTest {
            val endpoint = ServerEndpoint("192.168.0.20", 9443, isLocal = true)
            coEvery { connectToServerUseCase(endpoint, "User1", "4821") } returns Result.success(Unit)
            val component = createComponent()

            component.onIntent(ConnectionIntent.UpdateNickname("User1"))
            component.onIntent(ConnectionIntent.UpdatePin("4821"))
            component.onIntent(ConnectionIntent.ConnectToDiscovered(ServerNode("PTT-LAN-Host", endpoint)))
            advanceUntilIdle()

            coVerify(exactly = 1) { connectToServerUseCase(endpoint, "User1", "4821") }
        }

    @Test
    fun `connecting navigates even while the screen collects the effects`() =
        runTest {
            // Both used to collect one Channel, which hands each item to a single collector: the screen, which
            // started first, took the navigation and the first tap never left the connection screen.
            val status = MutableStateFlow(ConnectionStatus.Disconnected)
            every { observeConnectionStatusUseCase() } returns status
            val component = createComponent()
            backgroundScope.launch { component.effects.collect { } }
            advanceUntilIdle()

            status.value = ConnectionStatus.Connected

            withTimeout(1_000) { component.navigateToChannelList.first() }
        }
}
