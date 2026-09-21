package com.pttlan.core.network.discovery

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.runningFold

data class DiscoveredServer(
    val name: String,
    val host: String,
    val port: Int,
)

/** What the platform browser reports: a server resolved, or a server that left the network. */
sealed interface DiscoveryEvent {
    data class Found(
        val server: DiscoveredServer,
    ) : DiscoveryEvent

    data class Lost(
        val name: String,
    ) : DiscoveryEvent
}

/**
 * The servers on the network right now. Without the `Lost` side a room that ended stayed listed on every
 * other device until someone searched again.
 */
fun Flow<DiscoveryEvent>.currentServers(): Flow<List<DiscoveredServer>> =
    runningFold(emptyList<DiscoveredServer>()) { servers, event ->
        when (event) {
            // Found again replaces the entry: a host that came back may have a new address.
            is DiscoveryEvent.Found -> servers.filterNot { it.name == event.server.name } + event.server

            is DiscoveryEvent.Lost -> servers.filterNot { it.name == event.name }
        }
    }.drop(1)

expect class ServerDiscoveryService() {
    fun discover(): Flow<DiscoveryEvent>

    fun stopDiscovery()
}
