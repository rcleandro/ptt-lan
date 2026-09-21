package com.pttlan.core.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import co.touchlab.kermit.Logger

/** Whether capture and playback go through the headset's call profile; read when they open their streams. */
internal object CallRoute {
    @Volatile
    var active = false
}

class AndroidHeadsetMicRoute(
    context: Context,
) : HeadsetMicRoute {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    override fun enable(): Boolean {
        val headset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) findHeadset() else null
        val switched = headset != null && switchTo(headset)
        CallRoute.active = switched
        return switched
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun findHeadset(): AudioDeviceInfo? {
        val devices = audioManager.availableCommunicationDevices
        Logger.withTag("audio").d { "Communication devices: ${devices.map { "${it.productName} (type ${it.type})" }}" }
        return devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun switchTo(headset: AudioDeviceInfo): Boolean {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        val switched = audioManager.setCommunicationDevice(headset)
        if (!switched) audioManager.mode = AudioManager.MODE_NORMAL
        return switched
    }

    override fun disable() {
        if (!CallRoute.active) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) audioManager.clearCommunicationDevice()
        audioManager.mode = AudioManager.MODE_NORMAL
        CallRoute.active = false
    }
}
