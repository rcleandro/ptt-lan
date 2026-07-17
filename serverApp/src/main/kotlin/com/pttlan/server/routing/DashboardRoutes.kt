package com.pttlan.server.routing

import com.pttlan.server.channel.ChannelRegistry
import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.lang.management.ManagementFactory
import kotlin.system.exitProcess

@Serializable
data class ServerHealthDto(
    val uptimeMs: Long,
    val memoryUsedMb: Long,
    val memoryTotalMb: Long,
    val activeThreads: Int,
)

@Serializable
data class DashboardMetricsDto(
    val serverHealth: ServerHealthDto,
    val globalConnections: Int,
    val channels: List<DashboardChannelDto>,
    val logs: List<DashboardLogEventDto>,
    val speakerTimes: List<SpeakerTimeDto>,
    val timeSeries: List<TimeSeriesPointDto>,
)

@Serializable
data class TimeSeriesPointDto(
    val timestampMs: Long,
    val bytesTransferred: Long,
    val pttStarts: Int,
    val slowConnections: Int,
)

@Serializable
data class SpeakerTimeDto(
    val nickname: String,
    val totalTimeSeconds: Long,
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

@Suppress("MagicNumber")
fun Routing.dashboardRoutes() {
    val channelRegistry by inject<ChannelRegistry>()

    staticResources("/admin", "static")

    route("/api/admin") {
        get("/metrics") {
            val uptime = ManagementFactory.getRuntimeMXBean().uptime
            val memoryUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)
            val memoryTotal = Runtime.getRuntime().maxMemory() / (1024 * 1024)
            val threads = ManagementFactory.getThreadMXBean().threadCount
            val serverHealth = ServerHealthDto(uptime, memoryUsed, memoryTotal, threads)

            val activeChannels = channelRegistry.getActiveChannelsInfo()
            val globalConnections = channelRegistry.getGlobalConnectionsCount()
            val recentLogs = channelRegistry.getRecentLogs()
            val speakerTimes = channelRegistry.getSpeakerTimes()
            val timeSeries = channelRegistry.getTimeSeries()

            call.respond(
                DashboardMetricsDto(
                    serverHealth = serverHealth,
                    globalConnections = globalConnections,
                    channels = activeChannels,
                    logs = recentLogs,
                    speakerTimes = speakerTimes,
                    timeSeries = timeSeries,
                ),
            )
        }

        post("/system/restart") {
            channelRegistry.resetServer()
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        post("/system/shutdown") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
            Thread {
                Thread.sleep(500)
                exitProcess(0)
            }.start()
        }

        post("/channels/{id}/kick/{userId}") {
            val channelId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.parameters["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            if (channelRegistry.kickUser(channelId, userId)) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("status" to "not_found"))
            }
        }

        post("/channels/{id}/delete") {
            val channelId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            if (channelRegistry.closeChannel(channelId)) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("status" to "not_found"))
            }
        }
    }
}
