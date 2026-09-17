package com.pttlan.server.routing

import com.pttlan.server.channel.ChannelRegistry
import com.sun.management.OperatingSystemMXBean
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.lang.management.ManagementFactory
import kotlin.system.exitProcess

private const val SHUTDOWN_DELAY_MS = 500L

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

/**
 * Admin credential: Basic auth password from `ptt.adminPassword` (env `PTT_ADMIN_PASSWORD`).
 * When it is absent the panel stays open for reading, but every write route is disabled.
 */
fun Application.adminPassword(): String? =
    environment.config
        .propertyOrNull("ptt.adminPassword")
        ?.getString()
        ?.takeIf { it.isNotBlank() }

@Suppress("MagicNumber")
fun Routing.dashboardRoutes(adminEnabled: Boolean) {
    val channelRegistry by inject<ChannelRegistry>()

    authenticate("auth-admin", optional = !adminEnabled) {
        staticResources("/admin", "static")

        route("/api/admin") {
            get("/metrics") {
                val uptime = ManagementFactory.getRuntimeMXBean().uptime
                val runtime = Runtime.getRuntime()
                val memoryUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                val memoryTotal = runtime.maxMemory() / (1024 * 1024)
                val threads = ManagementFactory.getThreadMXBean().threadCount

                val osBean = ManagementFactory.getOperatingSystemMXBean()
                val rawCpuLoad =
                    if (osBean is OperatingSystemMXBean) {
                        osBean.processCpuLoad * 100.0
                    } else {
                        0.0
                    }
                val cpuLoad = rawCpuLoad.takeIf { it >= 0.0 && !it.isNaN() }?.coerceAtMost(100.0) ?: 0.0

                val serverHealth = ServerHealthDto(uptime, memoryUsed, memoryTotal, threads, cpuLoad)

                call.respond(
                    DashboardMetricsDto(
                        serverHealth = serverHealth,
                        globalConnections = channelRegistry.getGlobalConnectionsCount(),
                        channels = channelRegistry.getActiveChannelsInfo(),
                        logs = channelRegistry.getRecentLogs(),
                        speakerTimes = channelRegistry.getSpeakerTimes(),
                        timeSeries = channelRegistry.getTimeSeries(),
                    ),
                )
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
                call.respondText(csv, ContentType.Text.CSV)
            }

            if (adminEnabled) {
                adminWriteRoutes(channelRegistry)
            }
        }
    }
}

private fun Route.adminWriteRoutes(channelRegistry: ChannelRegistry) {
    post("/system/restart") {
        channelRegistry.resetServer()
        call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
    }

    post("/system/shutdown") {
        call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        Thread {
            Thread.sleep(SHUTDOWN_DELAY_MS)
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
