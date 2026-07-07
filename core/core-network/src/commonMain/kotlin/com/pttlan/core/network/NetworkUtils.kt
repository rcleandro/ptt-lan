package com.pttlan.core.network

fun normalizeHost(host: String): String {
    val trimmed = host.trim().removeSuffix(".")
    val regex = Regex("""^(\d{1,3})-(\d{1,3})-(\d{1,3})-(\d{1,3})\.local$""")
    val match = regex.matchEntire(trimmed)
    return if (match != null) {
        val (p1, p2, p3, p4) = match.destructured
        "$p1.$p2.$p3.$p4"
    } else {
        trimmed
    }
}
