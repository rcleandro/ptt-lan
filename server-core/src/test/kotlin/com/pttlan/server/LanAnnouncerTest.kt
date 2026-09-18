package com.pttlan.server

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanAnnouncerTest {
    @Test
    fun `announces on the physical interfaces only`() {
        listOf("en0", "eth0", "wlan0", "wlp2s0").forEach { assertTrue(isValidInterfaceName(it), it) }
        listOf("docker0", "utun3", "tailscale0", "awdl0", "llw0", "br-1a2b", "vboxnet0", "vmnet8", "VMnet1")
            .forEach { assertFalse(isValidInterfaceName(it), it) }
    }
}
