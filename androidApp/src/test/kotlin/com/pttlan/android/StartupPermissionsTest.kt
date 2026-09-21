package com.pttlan.android

import android.Manifest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StartupPermissionsTest {
    @Test
    fun androidSeventeenAsksForTheLocalNetwork() {
        // Found on a watch with Android 17: without it the app never saw nor reached a server on the LAN
        assertTrue(Manifest.permission.ACCESS_LOCAL_NETWORK in startupPermissions(sdkInt = 37))
    }

    @Test
    fun olderReleasesDoNotAskForAPermissionTheyDoNotHave() {
        assertFalse(Manifest.permission.ACCESS_LOCAL_NETWORK in startupPermissions(sdkInt = 36))
        assertTrue(Manifest.permission.RECORD_AUDIO in startupPermissions(sdkInt = 26))
    }
}
