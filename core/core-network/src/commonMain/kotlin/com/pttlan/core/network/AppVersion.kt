package com.pttlan.core.network

/**
 * Version the client reports to the server in the `/ws` handshake, shown in the admin panel so an outdated
 * device can be spotted. Bump it on release; 21.5 adds the protocol version next to it.
 */
const val APP_VERSION = "1.0"
