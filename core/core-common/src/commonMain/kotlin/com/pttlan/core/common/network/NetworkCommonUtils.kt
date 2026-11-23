package com.pttlan.core.common.network

import com.pttlan.core.common.DEFAULT_INTERNET_PORT
import com.pttlan.core.common.DEFAULT_SERVER_PORT

fun isLocalNetwork(host: String): Boolean {
    // mDNS names can arrive fully qualified ("ptt-server.local."), e.g. from NSNetService.hostName on iOS
    val h = host.trim().removeSuffix(".").lowercase()
    if (h == "localhost" || h == "127.0.0.1" || h.endsWith(".local")) return true
    val regex = Regex("""^(10\.\d{1,3}\.\d{1,3}\.\d{1,3})|(192\.168\.\d{1,3}\.\d{1,3})|(172\.(1[6-9]|2\d|3[0-1])\.\d{1,3}\.\d{1,3})$""")
    return regex.matches(h)
}

private const val MAX_PORT = 65535

data class HostAndPort(
    val host: String,
    val port: Int,
)

/**
 * Reads what the user typed in the manual connection field: `host`, `host:port`, or a pasted URL such as
 * `https://ptt.example.com/`. With no port, a LAN host gets the server's own port and an internet host gets
 * HTTPS's, where a reverse proxy or a tunnel answers. Returns null when the host is blank or the port is not valid.
 */
fun parseHostAndPort(input: String): HostAndPort? {
    val address = input.trim().substringAfter("://").substringBefore('/')
    // An unbracketed IPv6 literal has several colons and no port to split off
    val hasPort = address.count { it == ':' } == 1
    val host = if (hasPort) address.substringBefore(':') else address
    val port =
        when {
            hasPort -> address.substringAfter(':').toIntOrNull()?.takeIf { it in 1..MAX_PORT }
            isLocalNetwork(host) -> DEFAULT_SERVER_PORT
            else -> DEFAULT_INTERNET_PORT
        }
    return if (host.isBlank() || port == null) null else HostAndPort(host, port)
}
