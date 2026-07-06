package com.pttlan.core.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class AndroidMicrophonePermissionManager(
    private val context: Context,
) : MicrophonePermissionManager {
    override suspend fun isGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    override suspend fun request(): Boolean {
        return isGranted()
    }
}
