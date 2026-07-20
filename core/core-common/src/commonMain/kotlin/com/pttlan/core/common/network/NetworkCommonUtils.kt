package com.pttlan.core.common.network

fun isLocalNetwork(host: String): Boolean {
    val h = host.trim().lowercase()
    if (h == "localhost" || h == "127.0.0.1" || h.endsWith(".local")) return true
    val regex = Regex("""^(10\.\d{1,3}\.\d{1,3}\.\d{1,3})|(192\.168\.\d{1,3}\.\d{1,3})|(172\.(1[6-9]|2\d|3[0-1])\.\d{1,3}\.\d{1,3})$""")
    return regex.matches(h)
}
