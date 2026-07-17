package com.pttlan.server.routing

import com.pttlan.server.channel.ChannelRegistry
import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
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
    val cpuLoadPercent: Double,
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
    val cpuLoadPercent: Double,
    val memoryUsedPercent: Double,
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
    val ipAddress: String,
    val appVersion: String,
    val pingMs: Long,
)

@Suppress("MagicNumber", "LongMethod")
fun Routing.dashboardRoutes() {
    val channelRegistry by inject<ChannelRegistry>()

    staticResources("/admin", "static")

    route("/api/admin") {
        get("/metrics") {
            val uptime = ManagementFactory.getRuntimeMXBean().uptime
            val memoryUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)
            val memoryTotal = Runtime.getRuntime().maxMemory() / (1024 * 1024)
            val threads = ManagementFactory.getThreadMXBean().threadCount

            val osBean = ManagementFactory.getOperatingSystemMXBean()
            val rawCpuLoad =
                if (osBean is com.sun.management.OperatingSystemMXBean) {
                    osBean.processCpuLoad * 100.0
                } else {
                    0.0
                }
            val cpuLoad = rawCpuLoad.takeIf { it >= 0.0 && !it.isNaN() }?.coerceAtMost(100.0) ?: 0.0

            val serverHealth = ServerHealthDto(uptime, memoryUsed, memoryTotal, threads, cpuLoad)

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

        post("/system/broadcast") {
            @Serializable
            data class BroadcastRequest(
                val message: String,
            )
            val req = call.receive<BroadcastRequest>()
            channelRegistry.broadcastGlobalAlert(req.message)
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        get("/logs/csv") {
            val logs = channelRegistry.getRecentLogs()
            val csv =
                buildString {
                    appendLine("Timestamp,Channel,Participant,Event")
                    logs.forEach {
                        appendLine("${it.timestamp},${it.channelId},${it.participantName},${it.eventType}")
                    }
                }
            call.respondText(csv, io.ktor.http.ContentType.Text.CSV)
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
