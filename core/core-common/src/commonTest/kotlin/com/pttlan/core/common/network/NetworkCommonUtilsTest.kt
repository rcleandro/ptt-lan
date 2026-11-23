package com.pttlan.core.common.network

import com.pttlan.core.common.DEFAULT_INTERNET_PORT
import com.pttlan.core.common.DEFAULT_SERVER_PORT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetworkCommonUtilsTest {
    @Test
    fun testIsLocalNetworkWithLocalhost() {
        assertTrue(isLocalNetwork("localhost"))
        assertTrue(isLocalNetwork("127.0.0.1"))
    }

    @Test
    fun testIsLocalNetworkWithMdns() {
        assertTrue(isLocalNetwork("ptt-server.local"))
        assertTrue(isLocalNetwork("MACBOOK.LOCAL"))
        // Bonjour resolves to a fully qualified name, with the root dot
        assertTrue(isLocalNetwork("ptt-server.local."))
    }

    @Test
    fun testIsLocalNetworkWithPrivateIpRanges() {
        // Classe A
        assertTrue(isLocalNetwork("10.0.0.1"))
        assertTrue(isLocalNetwork("10.255.255.254"))

        // Classe B
        assertTrue(isLocalNetwork("172.16.0.1"))
        assertTrue(isLocalNetwork("172.31.255.255"))

        // Classe C
        assertTrue(isLocalNetwork("192.168.0.1"))
        assertTrue(isLocalNetwork("192.168.254.254"))
    }

    @Test
    fun testIsLocalNetworkWithPublicInternet() {
        assertFalse(isLocalNetwork("8.8.8.8"))
        assertFalse(isLocalNetwork("1.1.1.1"))
        assertFalse(isLocalNetwork("google.com"))
        assertFalse(isLocalNetwork("ptt.meuservidor.com"))
    }

    @Test
    fun testIsLocalNetworkWithPublicIpsThatLookSimilar() {
        assertFalse(isLocalNetwork("11.0.0.1")) // fora do range 10.x
        assertFalse(isLocalNetwork("172.15.0.1")) // fora do range 172.16-31
        assertFalse(isLocalNetwork("172.32.0.1")) // fora do range 172.16-31
        assertFalse(isLocalNetwork("192.169.0.1")) // fora do range 192.168
    }

    @Test
    fun aHostWithoutPortGetsTheServerPortOnTheLanAndHttpsOnTheInternet() {
        assertEquals(HostAndPort("192.168.1.48", DEFAULT_SERVER_PORT), parseHostAndPort("192.168.1.48"))
        assertEquals(HostAndPort("ptt-server.local", DEFAULT_SERVER_PORT), parseHostAndPort(" ptt-server.local "))
        assertEquals(HostAndPort("ptt.example.com", DEFAULT_INTERNET_PORT), parseHostAndPort("ptt.example.com"))
    }

    @Test
    fun anExplicitPortWinsOverTheDefault() {
        assertEquals(HostAndPort("ptt.example.com", 9443), parseHostAndPort("ptt.example.com:9443"))
        assertEquals(HostAndPort("192.168.1.48", 8443), parseHostAndPort("192.168.1.48:8443"))
    }

    @Test
    fun aPastedUrlKeepsOnlyTheHostAndPort() {
        assertEquals(HostAndPort("ptt.example.com", DEFAULT_INTERNET_PORT), parseHostAndPort("https://ptt.example.com/"))
        assertEquals(HostAndPort("ptt.example.com", 8443), parseHostAndPort("wss://ptt.example.com:8443/ws"))
    }

    @Test
    fun anIpv6LiteralIsNotSplitAtItsColons() {
        assertEquals(HostAndPort("fe80::1", DEFAULT_INTERNET_PORT), parseHostAndPort("fe80::1"))
    }

    @Test
    fun aBlankHostOrABadPortIsRejected() {
        assertNull(parseHostAndPort(""))
        assertNull(parseHostAndPort(":9443"))
        assertNull(parseHostAndPort("ptt.example.com:"))
        assertNull(parseHostAndPort("ptt.example.com:abc"))
        assertNull(parseHostAndPort("ptt.example.com:0"))
        assertNull(parseHostAndPort("ptt.example.com:65536"))
    }
}
