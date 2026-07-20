package com.pttlan.feature.connection

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerEndpoint
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            modules(module {
                single<Settings> { settings }
            })
        }

        every { observeConnectionStatusUseCase() } returns emptyFlow()
        every { discoverServersUseCase() } returns emptyFlow()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    private fun createComponent(): ConnectionComponent {
        val lifecycle = LifecycleRegistry()
        return ConnectionComponent(
            componentContext = DefaultComponentContext(lifecycle),
            observeConnectionStatusUseCase = observeConnectionStatusUseCase,
            discoverServersUseCase = discoverServersUseCase,
            connectToServerUseCase = connectToServerUseCase
        )
    }

    @Test
    fun `when ConnectToManualIp with valid nickname and IP, should call use case with correct isLocal flag`() = runTest {
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
    fun `when ConnectToManualIp with internet domain, should call use case with isLocal false`() = runTest {
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
    fun `when ConnectToManualIp throws TimeoutCancellationException, should emit ShowError effect`() = runTest {
        val component = createComponent()
        
        // Arrange
        component.onIntent(ConnectionIntent.UpdateNickname("User1"))
        component.onIntent(ConnectionIntent.UpdateManualIp("10.0.0.1"))
        
        val expectedEndpoint = ServerEndpoint("10.0.0.1", 9443, isLocal = true)
        coEvery { connectToServerUseCase(expectedEndpoint, "User1") } returns Result.failure(TimeoutCancellationException("Timeout"))

        // Act
        component.onIntent(ConnectionIntent.ConnectToManualIp("10.0.0.1"))
        advanceUntilIdle()

        // Assert
        val effect = component.effects.first()
        assertTrue(effect is ConnectionEffect.ShowError)
        assertTrue(effect.message.contains("Tempo de conexão excedido"))
    }
}
