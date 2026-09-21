package com.pttlan.core.audio

/**
 * Routes capture and playback through a Bluetooth headset's call profile, so the headset's microphone is used
 * (26.2, ADR 0012). It lowers everything to phone quality, so it is opt-in and held for a whole session.
 */
interface HeadsetMicRoute {
    /** Switches to the headset; false when no headset with a microphone is connected, and nothing changes. */
    fun enable(): Boolean

    fun disable()
}

/** Platforms that follow the system's default devices (the desktop). */
object NoHeadsetMicRoute : HeadsetMicRoute {
    override fun enable() = false

    override fun disable() = Unit
}
