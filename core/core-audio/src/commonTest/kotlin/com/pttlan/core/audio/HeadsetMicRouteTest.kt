package com.pttlan.core.audio

import kotlin.test.Test
import kotlin.test.assertFalse

class HeadsetMicRouteTest {
    @Test
    fun theDesktopKeepsTheSystemDevices() {
        // Desktop follows the system's default input and output; the option has nothing to switch there
        assertFalse(NoHeadsetMicRoute.enable())
        NoHeadsetMicRoute.disable()
    }
}
