package com.pttlan.core.navigation

import com.pttlan.domain.ptt.repository.ConnectionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import com.pttlan.core.designsystem.components.ConnectionStatus as BadgeStatus

class ConnectionBadgeTest {
    @Test
    fun aClientStillTryingReadsAsReconnectingAndOnlyAGiveUpAsOffline() {
        assertEquals(BadgeStatus.Online, badgeStatus(ConnectionStatus.Connected))
        assertEquals(BadgeStatus.Reconnecting, badgeStatus(ConnectionStatus.Connecting))
        assertEquals(BadgeStatus.Reconnecting, badgeStatus(ConnectionStatus.Reconnecting))
        assertEquals(BadgeStatus.Offline, badgeStatus(ConnectionStatus.Disconnected))
    }
}
