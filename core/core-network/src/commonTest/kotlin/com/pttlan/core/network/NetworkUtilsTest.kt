package com.pttlan.core.network

import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkUtilsTest {
    @Test
    fun mdnsHostNameBecomesItsIpAddress() {
        assertEquals("192.168.0.12", normalizeHost(" 192-168-0-12.local. "))
    }

    @Test
    fun otherHostsAreOnlyTrimmed() {
        assertEquals("10.0.0.5", normalizeHost("10.0.0.5"))
        assertEquals("my-server.local", normalizeHost("my-server.local."))
    }
}
