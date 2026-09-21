package com.pttlan.android

import com.pttlan.domain.ptt.repository.ConnectionStatus

/**
 * Whether the foreground service must run: true starts it, false stops it, null leaves it as it is (a
 * reconnect in progress should not tear it down). Hosting always keeps it — the room lives in this process,
 * even when this user is not in a channel (24.5).
 */
fun listeningServiceWanted(
    status: ConnectionStatus,
    activeChannel: String?,
    alwaysListening: Boolean,
    hosting: Boolean,
): Boolean? =
    when {
        hosting -> true
        status == ConnectionStatus.Connected && activeChannel != null && alwaysListening -> true
        status == ConnectionStatus.Disconnected || activeChannel == null || !alwaysListening -> false
        else -> null
    }
