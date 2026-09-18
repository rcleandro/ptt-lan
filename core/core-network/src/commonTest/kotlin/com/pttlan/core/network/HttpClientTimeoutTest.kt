package com.pttlan.core.network

import kotlin.test.Test
import kotlin.test.assertTrue

class HttpClientTimeoutTest {
    @Test
    fun socketTimeoutSurvivesSeveralMissedPings() {
        // They were equal (5s each), so one late ping killed a healthy connection on a silent channel.
        // Whoever changes either number has to look at the other one.
        assertTrue(
            SOCKET_TIMEOUT_MS >= WEBSOCKET_PING_INTERVAL_MS * 3,
            "socket timeout ($SOCKET_TIMEOUT_MS ms) must tolerate a few missed pings " +
                "(interval $WEBSOCKET_PING_INTERVAL_MS ms)",
        )
    }
}
