package com.pttlan.data.ptt.repository

import com.pttlan.core.audio.HeadsetMicRoute
import com.pttlan.core.datastore.SettingsKeys
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HeadsetMicSessionTest {
    private val calls = mutableListOf<String>()
    private val route =
        object : HeadsetMicRoute {
            override fun enable(): Boolean {
                calls += "enable"
                return true
            }

            override fun disable() {
                calls += "disable"
            }
        }
    private val status = MutableStateFlow(ConnectionStatus.Disconnected)
    private val settings = MapSettings()

    @Test
    fun turnedOnItHoldsTheHeadsetForTheWholeSession() =
        runTest {
            settings.putBoolean(SettingsKeys.USE_HEADSET_MIC, true)
            HeadsetMicSession(status, settings, route, StandardTestDispatcher(testScheduler))
            advanceUntilIdle()
            calls.clear()

            status.value = ConnectionStatus.Connected
            advanceUntilIdle()
            // A reconnect is the same session: switching profiles back and forth would drop audio on each drop
            status.value = ConnectionStatus.Reconnecting
            advanceUntilIdle()
            status.value = ConnectionStatus.Connected
            advanceUntilIdle()
            status.value = ConnectionStatus.Disconnected
            advanceUntilIdle()

            assertEquals(listOf("enable", "disable"), calls)
        }

    @Test
    fun turnedOffItNeverTakesTheHeadset() =
        runTest {
            HeadsetMicSession(status, settings, route, StandardTestDispatcher(testScheduler))

            status.value = ConnectionStatus.Connected
            advanceUntilIdle()

            assertEquals(false, "enable" in calls)
        }
}
