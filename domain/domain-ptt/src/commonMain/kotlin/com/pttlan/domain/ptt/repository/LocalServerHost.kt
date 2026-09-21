package com.pttlan.domain.ptt.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Runs the PTT server inside this app so the device hosts the channel itself (host mode, ADR 0010).
 * Only registered on platforms that can host; elsewhere there is no binding and the UI hides the option.
 */
interface LocalServerHost {
    /** Whether this device is hosting a room right now: leaving the room then asks before ending it. */
    val isHosting: StateFlow<Boolean>

    /**
     * Starts the server (a no-op when it is already running) and returns where this device connects to it.
     * A non-blank [pin] closes the room: only who sends the same PIN at login gets in.
     */
    suspend fun start(
        serviceName: String,
        pin: String?,
    ): Result<ServerEndpoint>

    fun stop()
}
