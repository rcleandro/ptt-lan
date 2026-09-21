package com.pttlan.data.ptt.repository

import co.touchlab.kermit.Logger
import com.pttlan.core.audio.HeadsetMicRoute
import com.pttlan.core.datastore.SettingsDefaults
import com.pttlan.core.datastore.SettingsKeys
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Holds the headset's microphone for a whole session when the user turned it on (ADR 0012): on when the
 * connection is made, off when it ends. A reconnect is still the same session, so the route does not flap.
 * The setting is read when a session starts, so changing it applies to the next connection.
 */
class HeadsetMicSession(
    connectionStatus: Flow<ConnectionStatus>,
    private val settings: Settings,
    private val route: HeadsetMicRoute,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val logger = Logger.withTag("audio")

    init {
        connectionStatus
            .map { it == ConnectionStatus.Connected || it == ConnectionStatus.Reconnecting }
            .distinctUntilChanged()
            .onEach { inSession ->
                val wanted = settings.getBoolean(SettingsKeys.USE_HEADSET_MIC, SettingsDefaults.USE_HEADSET_MIC)
                if (inSession && wanted) {
                    val switched = route.enable()
                    logger.i { if (switched) "Headset microphone in use" else "Headset microphone wanted but no headset found" }
                } else {
                    route.disable()
                }
            }.launchIn(CoroutineScope(dispatcher))
    }
}
