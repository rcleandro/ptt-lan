package com.pttlan.core.network

import com.pttlan.core.common.ServerCertificateChangedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 30.5: trust on first use for LAN servers, whose self-signed certificates no authority vouches for. */
class CertificatePinsTest {
    private val first = sha256Hex("first certificate".encodeToByteArray())
    private val second = sha256Hex("second certificate".encodeToByteArray())

    @Test
    fun `sha256 matches the reference value`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256Hex("abc".encodeToByteArray()),
        )
    }

    @Test
    fun `the first certificate of a host is trusted and kept`() {
        val pins = CertificatePins()

        assertTrue(pins.verify("192.168.1.10", 9443, first))
        assertTrue(pins.verify("192.168.1.10", 9443, first))
        assertEquals(certificateCode(first), pins.codeFor("192.168.1.10", 9443))
    }

    @Test
    fun `another certificate for a known host is refused and reported with both codes`() {
        val pins = CertificatePins()
        pins.verify("192.168.1.10", 9443, first)

        assertFalse(pins.verify("192.168.1.10", 9443, second))

        val change = assertIs<ServerCertificateChangedException>(pins.changeFor("192.168.1.10", 9443))
        assertEquals(certificateCode(first), change.previousCode)
        assertEquals(certificateCode(second), change.newCode)
        assertNull(pins.changeFor("192.168.1.11", 9443), "another host is not affected")
    }

    @Test
    fun `trusting the new certificate replaces the old one`() {
        val pins = CertificatePins()
        pins.verify("192.168.1.10", 9443, first)
        pins.verify("192.168.1.10", 9443, second)

        pins.trustChanged("192.168.1.10", 9443)

        assertTrue(pins.verify("192.168.1.10", 9443, second))
        assertFalse(pins.verify("192.168.1.10", 9443, first))
    }

    @Test
    fun `the code is short enough to read aloud`() {
        assertEquals("BA78-16BF", certificateCode("ba7816bf8f01cfea"))
    }
}
