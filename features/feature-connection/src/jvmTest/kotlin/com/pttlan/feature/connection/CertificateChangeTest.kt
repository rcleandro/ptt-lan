package com.pttlan.feature.connection

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.core.common.ServerCertificateChangedException
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.domain.ptt.usecase.ConnectToServerUseCase
import com.pttlan.domain.ptt.usecase.DiscoverServersUseCase
import com.pttlan.domain.ptt.usecase.ObserveConnectionStatusUseCase
import com.pttlan.domain.ptt.usecase.TrustServerCertificateUseCase
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
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
import kotlin.test.assertNull

/** 30.5: a LAN server whose certificate changed is refused until the user compares the codes and trusts it. */
@OptIn(ExperimentalCoroutinesApi::class)
class CertificateChangeTest {
    private val observeConnectionStatusUseCase: ObserveConnectionStatusUseCase = mockk()
    private val discoverServersUseCase: DiscoverServersUseCase = mockk()
    private val connectToServerUseCase: ConnectToServerUseCase = mockk()
    private val trustServerCertificateUseCase: TrustServerCertificateUseCase = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val endpoint = ServerEndpoint("192.168.0.50", 9443, isLocal = true)
    private val changed = ServerCertificateChangedException("192.168.0.50", 9443, "A1B2-C3D4", "E5F6-0718")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        startKoin { modules(module { single<Settings> { MapSettings() } }) }
        every { observeConnectionStatusUseCase() } returns emptyFlow()
        every { discoverServersUseCase() } returns emptyFlow()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    private fun createComponent() =
        ConnectionComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            observeConnectionStatusUseCase = observeConnectionStatusUseCase,
            discoverServersUseCase = discoverServersUseCase,
            connectToServerUseCase = connectToServerUseCase,
            trustServerCertificateUseCase = trustServerCertificateUseCase,
        )

    private fun ConnectionComponent.connectTo(ip: String) {
        onIntent(ConnectionIntent.UpdateNickname("Ana"))
        onIntent(ConnectionIntent.UpdateManualIp(ip))
        onIntent(ConnectionIntent.ConnectToManualIp(ip))
    }

    @Test
    fun `a changed certificate asks the user with both codes instead of a plain error`() =
        runTest {
            coEvery { connectToServerUseCase(endpoint, "Ana", null) } returns Result.failure(changed)
            val component = createComponent()

            component.connectTo("192.168.0.50")
            advanceUntilIdle()

            val change = component.state.value.certificateChange
            assertEquals("A1B2-C3D4", change?.previousCode)
            assertEquals("E5F6-0718", change?.newCode)
        }

    @Test
    fun `trusting the new certificate stores it and connects again`() =
        runTest {
            coEvery { connectToServerUseCase(endpoint, "Ana", null) } returnsMany listOf(Result.failure(changed), Result.success(Unit))
            val component = createComponent()
            component.connectTo("192.168.0.50")
            advanceUntilIdle()

            component.onIntent(ConnectionIntent.TrustNewCertificate)
            advanceUntilIdle()

            verify(exactly = 1) { trustServerCertificateUseCase(endpoint) }
            coVerify(exactly = 2) { connectToServerUseCase(endpoint, "Ana", null) }
            assertNull(component.state.value.certificateChange)
        }

    @Test
    fun `cancelling keeps the old certificate and does not connect`() =
        runTest {
            coEvery { connectToServerUseCase(endpoint, "Ana", null) } returns Result.failure(changed)
            val component = createComponent()
            component.connectTo("192.168.0.50")
            advanceUntilIdle()

            component.onIntent(ConnectionIntent.DismissCertificateChange)
            advanceUntilIdle()

            verify(exactly = 0) { trustServerCertificateUseCase(any()) }
            coVerify(exactly = 1) { connectToServerUseCase(endpoint, "Ana", null) }
            assertNull(component.state.value.certificateChange)
        }
}
