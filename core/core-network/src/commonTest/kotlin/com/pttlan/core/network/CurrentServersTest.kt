package com.pttlan.core.network

import com.pttlan.core.network.discovery.DiscoveredServer
import com.pttlan.core.network.discovery.DiscoveryEvent.Found
import com.pttlan.core.network.discovery.DiscoveryEvent.Lost
import com.pttlan.core.network.discovery.currentServers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrentServersTest {
    private val room = DiscoveredServer("PTT-LAN-Android", "192.168.1.230", 9443)
    private val other = DiscoveredServer("PTT-LAN-Desktop", "192.168.1.231", 9443)

    @Test
    fun aRoomThatEndsLeavesTheList() =
        runTest {
            val lists = flowOf(Found(room), Found(other), Lost(room.name)).currentServers().toList()

            assertEquals(listOf(listOf(room), listOf(room, other), listOf(other)), lists)
        }

    @Test
    fun aRoomFoundAgainKeepsOneEntryWithTheNewAddress() =
        runTest {
            val moved = room.copy(host = "192.168.1.240")

            val last = flowOf(Found(room), Found(moved)).currentServers().toList().last()

            assertEquals(listOf(moved), last)
        }
}
