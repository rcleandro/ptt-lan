package com.pttlan.android

import android.Manifest
import android.os.Build

/** Android 17, the first release that gates the local network behind a runtime permission (ADR 0011). */
private const val LOCAL_NETWORK_PERMISSION_SDK = 37

/** Runtime permissions asked for when the app opens. */
fun startupPermissions(sdkInt: Int): List<String> =
    buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        // Without it NSD only opens a system picker and connections to LAN addresses time out
        if (sdkInt >= LOCAL_NETWORK_PERMISSION_SDK) add(Manifest.permission.ACCESS_LOCAL_NETWORK)
    }
