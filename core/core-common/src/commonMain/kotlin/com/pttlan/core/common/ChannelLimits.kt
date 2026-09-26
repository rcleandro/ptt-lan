package com.pttlan.core.common

/** Port the server listens on, and the one the app connects to when none is given. */
const val DEFAULT_SERVER_PORT = 9443

/** Shortest room PIN: 6 characters take years to guess at the pace the room lockout allows (30.3). */
const val MIN_ROOM_PIN_LENGTH = 6

/** A hosted room announces itself on the LAN as this prefix plus the host's name. */
const val HOSTED_ROOM_PREFIX = "PTT-LAN-"

/** The channel every server keeps open, listed first. */
const val DEFAULT_CHANNEL_ID = "Geral"

/** Longest channel name: the app cuts it while typing and the server refuses longer ones (30.10). */
const val MAX_CHANNEL_NAME_LENGTH = 40

/** Most channels a server keeps open at once, so nobody floods everybody's list (30.10). */
const val MAX_CHANNELS = 50
