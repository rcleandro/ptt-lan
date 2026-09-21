package com.pttlan.core.network

fun normalizeHost(host: String): String {
    val trimmed = host.trim().removeSuffix(".")
    val regex = Regex("""^(\d{1,3})-(\d{1,3})-(\d{1,3})-(\d{1,3})\.local$""")
    val match = regex.matchEntire(trimmed)
    return match?.groupValues?.drop(1)?.joinToString(".") ?: trimmed
}
