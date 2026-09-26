package com.pttlan.feature.connection

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.core.common.MIN_ROOM_PIN_LENGTH
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.connection_error_pin_too_short
import com.pttlan.domain.ptt.repository.LocalServerHost
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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

/** 30.3: a short room PIN is refused on the screen, with the app's own message, before the host server sees it. */
@OptIn(ExperimentalCoroutinesApi::class)
class HostPinLengthTest {
    private val host: LocalServerHost = mockk(relaxed = true)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        startKoin { modules(module { single<Settings> { MapSettings() } }) }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `a pin shorter than the minimum is refused before hosting`() =
        runTest {
            val component =
                ConnectionComponent(
                    componentContext = DefaultComponentContext(LifecycleRegistry()),
                    observeConnectionStatusUseCase = mockk { every { this@mockk() } returns emptyFlow() },
                    discoverServersUseCase = mockk { every { this@mockk() } returns emptyFlow() },
                    connectToServerUseCase = mockk(relaxed = true),
                    trustServerCertificateUseCase = mockk(relaxed = true),
                    localServerHost = host,
                )
            component.onIntent(ConnectionIntent.UpdateNickname("Ana"))
            component.onIntent(ConnectionIntent.UpdatePin("4821"))

            component.onIntent(ConnectionIntent.HostServer)

            assertEquals(
                ConnectionEffect.ShowError(Res.string.connection_error_pin_too_short, listOf(MIN_ROOM_PIN_LENGTH)),
                component.effects.first(),
            )
            coVerify(exactly = 0) { host.start(any(), any()) }
        }
}
