package com.pttlan.desktop

import com.pttlan.domain.ptt.repository.LocalServerHost
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.server.PTT_PORT
import com.pttlan.server.PttHostServer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Host mode on the desktop: the server runs in this process and the app connects to it over loopback. */
class DesktopServerHost(
    private val server: PttHostServer = PttHostServer(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LocalServerHost {
    private val _isHosting = MutableStateFlow(false)
    override val isHosting: StateFlow<Boolean> = _isHosting.asStateFlow()

    override suspend fun start(
        serviceName: String,
        pin: String?,
    ): Result<ServerEndpoint> =
        withContext(ioDispatcher) {
            runCatching {
                server.start(serviceName, pin)
                _isHosting.value = true
                ServerEndpoint(host = "localhost", port = PTT_PORT, isLocal = true)
            }
        }

    override fun stop() {
        _isHosting.value = false
        server.stop()
    }
}
