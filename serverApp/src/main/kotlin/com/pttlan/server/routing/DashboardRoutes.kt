package com.pttlan.server.routing

import com.pttlan.server.channel.ChannelRegistry
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class DashboardMetricsDto(
    val globalConnections: Int,
    val channels: List<DashboardChannelDto>,
    val logs: List<DashboardLogEventDto>,
)

@Serializable
data class DashboardLogEventDto(
    val timestamp: Long,
    val channelId: String,
    val participantName: String,
    val eventType: String,
)

@Serializable
data class DashboardChannelDto(
    val id: String,
    val participantCount: Int,
    val currentSpeakerId: String?,
    val participants: List<DashboardParticipantDto>,
)

@Serializable
data class DashboardParticipantDto(
    val userId: String,
    val nickname: String,
    val isSpeaking: Boolean,
)

fun Routing.dashboardRoutes() {
    val channelRegistry by inject<ChannelRegistry>()

    staticResources("/admin", "static")

    route("/api/admin") {
        get("/metrics") {
            val activeChannels = channelRegistry.getActiveChannelsInfo()
            val globalConnections = channelRegistry.getGlobalConnectionsCount()
            val recentLogs = channelRegistry.getRecentLogs()

            call.respond(
                DashboardMetricsDto(
                    globalConnections = globalConnections,
                    channels = activeChannels,
                    logs = recentLogs,
                ),
            )
        }
    }
}
