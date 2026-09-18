package com.pttlan.desktop

import com.pttlan.domain.ptt.repository.LocalServerHost
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.server.PTT_PORT
import com.pttlan.server.PttHostServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Host mode on the desktop: the server runs in this process and the app connects to it over loopback. */
class DesktopServerHost(
    private val server: PttHostServer = PttHostServer(),
) : LocalServerHost {
    override suspend fun start(serviceName: String): Result<ServerEndpoint> =
        withContext(Dispatchers.IO) {
            runCatching {
                server.start(serviceName)
                ServerEndpoint(host = "localhost", port = PTT_PORT, isLocal = true)
            }
        }

    override fun stop() = server.stop()
}
