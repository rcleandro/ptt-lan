package com.pttlan.core.network.discovery

import kotlinx.coroutines.flow.Flow

data class DiscoveredServer(val name: String, val host: String, val port: Int)

expect class ServerDiscoveryService() {
    fun discover(): Flow<DiscoveredServer>
    fun stopDiscovery()
}
