package com.pttlan.core.common

/** Longest channel name: the app cuts it while typing and the server refuses longer ones (30.10). */
const val MAX_CHANNEL_NAME_LENGTH = 40

/** Most channels a server keeps open at once, so nobody floods everybody's list (30.10). */
const val MAX_CHANNELS = 50
