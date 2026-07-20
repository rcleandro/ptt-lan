package com.pttlan.core.common.network

import kotlin.test.Test
import kotlin.test.assertFalse
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
}
