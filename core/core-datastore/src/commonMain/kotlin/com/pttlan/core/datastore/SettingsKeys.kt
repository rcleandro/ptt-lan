package com.pttlan.core.datastore

/**
 * Every key stored in [com.russhwolf.settings.Settings], with the default that goes with it.
 *
 * The keys used to be string literals spread over components and repositories — `"allow_cache"` in eight
 * places, `"app_theme"` in twelve — so a typo silently created a second setting and a default could drift
 * between the screen that writes it and the code that reads it.
 */
object SettingsKeys {
    const val NICKNAME = "nickname"
    const val MANUAL_IP = "manualIp"
    const val DEVICE_ID = "device_id"

    const val APP_THEME = "app_theme"
    const val REDUCE_TRANSPARENCY = "reduce_transparency"

    const val ALLOW_CACHE = "allow_cache"
    const val CACHE_LOCATION = "cache_location"
    const val MAX_CACHE_SIZE_MB = "max_cache_size_mb"

    const val USE_OPUS = "use_opus"
    const val ALWAYS_LISTENING = "always_listening"
    const val USE_HEADSET_MIC = "use_headset_mic"

    const val PLAYBACK_SPEED = "playback_speed"

    /** Followed by `host:port`: the certificate fingerprint trusted on first use for that LAN server (30.5). */
    const val CERTIFICATE_PIN_PREFIX = "cert_pin_"
}

/** Defaults for the keys that have one, so reader and writer cannot disagree. */
object SettingsDefaults {
    const val APP_THEME = 0
    const val REDUCE_TRANSPARENCY = false
    const val ALLOW_CACHE = false
    const val CACHE_LOCATION = "Interno"
    const val MAX_CACHE_SIZE_MB = 500
    const val USE_OPUS = true
    const val ALWAYS_LISTENING = true

    /** Off: the headset's microphone drops all audio to phone quality (ADR 0012). */
    const val USE_HEADSET_MIC = false

    /** The history's replay speed. */
    const val PLAYBACK_SPEED = 1f
}
