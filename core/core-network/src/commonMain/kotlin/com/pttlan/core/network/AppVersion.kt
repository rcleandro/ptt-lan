package com.pttlan.core.network

import kotlinx.serialization.json.Json

/**
 * Version the client reports to the server in the `/ws` handshake, shown in the admin panel so an outdated
 * device can be spotted. Bump it on release.
 */
const val APP_VERSION = "1.0"

/**
 * Wire protocol version, sent in the handshake. The server refuses a client that does not match, instead of
 * letting it fail later on a message it cannot understand. Bump it on every breaking protocol change.
 */
const val PROTOCOL_VERSION = 1

/**
 * The one JSON configuration both sides use. `ignoreUnknownKeys` is what lets a new field ship without
 * breaking older clients, and `encodeDefaults = false` keeps the control frames small.
 */
val PttJson: Json =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
