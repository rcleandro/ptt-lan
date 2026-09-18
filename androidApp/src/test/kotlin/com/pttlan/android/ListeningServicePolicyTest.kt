package com.pttlan.android

import com.pttlan.domain.ptt.repository.ConnectionStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ListeningServicePolicyTest {
    @Test
    fun hostingKeepsTheServiceEvenOutsideAChannel() {
        assertEquals(true, listeningServiceWanted(ConnectionStatus.Disconnected, null, alwaysListening = false, hosting = true))
    }

    @Test
    fun withoutHostingTheRulesOfBeforeHold() {
        assertEquals(true, listeningServiceWanted(ConnectionStatus.Connected, "Geral", alwaysListening = true, hosting = false))
        assertEquals(false, listeningServiceWanted(ConnectionStatus.Connected, null, alwaysListening = true, hosting = false))
        assertEquals(false, listeningServiceWanted(ConnectionStatus.Connected, "Geral", alwaysListening = false, hosting = false))
        assertEquals(false, listeningServiceWanted(ConnectionStatus.Disconnected, "Geral", alwaysListening = true, hosting = false))
        assertEquals(null, listeningServiceWanted(ConnectionStatus.Reconnecting, "Geral", alwaysListening = true, hosting = false))
    }
}
