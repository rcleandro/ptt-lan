package com.pttlan.core.navigation

import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.core.designsystem.components.ConnectionStatus as BadgeStatus

/**
 * What the status badge on the screens shows. A client still retrying reads as "reconectando", not offline: the
 * screens stay while it retries. Shared by the Compose and the SwiftUI screens (32.4).
 */
fun badgeStatus(status: ConnectionStatus): BadgeStatus =
    when (status) {
        ConnectionStatus.Connected -> BadgeStatus.Online
        ConnectionStatus.Connecting, ConnectionStatus.Reconnecting -> BadgeStatus.Reconnecting
        ConnectionStatus.Disconnected -> BadgeStatus.Offline
    }
