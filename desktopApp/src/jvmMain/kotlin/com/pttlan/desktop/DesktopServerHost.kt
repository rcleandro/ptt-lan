package com.pttlan.desktop

import com.pttlan.domain.ptt.repository.LocalServerHost
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.server.PTT_PORT
import com.pttlan.server.PttHostServer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Host mode on the desktop: the server runs in this process and the app connects to it over loopback. */
class DesktopServerHost(
    private val server: PttHostServer = PttHostServer(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LocalServerHost {
    override suspend fun start(
        serviceName: String,
        pin: String?,
    ): Result<ServerEndpoint> =
        withContext(ioDispatcher) {
            runCatching {
                server.start(serviceName, pin)
                ServerEndpoint(host = "localhost", port = PTT_PORT, isLocal = true)
            }
        }

    override fun stop() = server.stop()
}
