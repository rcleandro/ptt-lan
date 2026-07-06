package com.pttlan.core.telemetry

import co.touchlab.kermit.Logger

interface AnalyticsTracker {
    fun trackEvent(
        eventName: String,
        params: Map<String, String> = emptyMap(),
    )
}

class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun trackEvent(
        eventName: String,
        params: Map<String, String>,
    ) {
        Logger.withTag("Analytics").d { "Tracked Event: $eventName, params: $params" }
    }
}
