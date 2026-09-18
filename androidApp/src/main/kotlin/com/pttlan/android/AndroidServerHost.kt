package com.pttlan.android

import android.content.Context
import com.pttlan.domain.ptt.repository.LocalServerHost
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.server.PTT_PORT
import com.pttlan.server.PttHostServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

/**
 * Host mode on Android (24.5): the server runs in this process, announced through NSD, and the app connects to
 * it over loopback. [isHosting] keeps the foreground service up; the service's "Stop" action ends the room.
 */
class AndroidServerHost(
    context: Context,
) : LocalServerHost {
    private val server = PttHostServer(announce = { port, name -> announceWithNsd(context, port, name) })

    private val _isHosting = MutableStateFlow(false)
    val isHosting: StateFlow<Boolean> = _isHosting.asStateFlow()

    override suspend fun start(
        serviceName: String,
        pin: String?,
    ): Result<ServerEndpoint> =
        withContext(Dispatchers.IO) {
            runCatching {
                server.start(serviceName, pin)
                _isHosting.value = true
                ServerEndpoint(host = "localhost", port = PTT_PORT, isLocal = true)
            }
        }

    /** Netty waits for its grace period while stopping, so it happens off the caller's (main) thread. */
    override fun stop() {
        _isHosting.value = false
        thread(name = "ptt-host-stop") { server.stop() }
    }
}
