package com.pttlan.server.routing

import com.pttlan.server.channel.ChannelRegistry
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Routing.dashboardRoutes() {
    val channelRegistry by inject<ChannelRegistry>()

    staticResources("/admin", "static")

    route("/api/admin") {
        get("/metrics") {
            val activeChannels = channelRegistry.getActiveChannelsInfo()
            val globalConnections = channelRegistry.getGlobalConnectionsCount()

            call.respond(
                mapOf(
                    "globalConnections" to globalConnections,
                    "channels" to activeChannels,
                ),
            )
        }
    }
}
