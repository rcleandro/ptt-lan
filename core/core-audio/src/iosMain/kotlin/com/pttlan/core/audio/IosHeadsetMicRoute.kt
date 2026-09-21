package com.pttlan.core.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionPortBluetoothHFP
import platform.AVFAudio.AVAudioSessionPortDescription
import platform.AVFAudio.availableInputs

@OptIn(ExperimentalForeignApi::class)
class IosHeadsetMicRoute : HeadsetMicRoute {
    override fun enable(): Boolean {
        headsetMicEnabled = true
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayAndRecord, sessionOptions(), null)
        val headset =
            session.availableInputs
                ?.filterIsInstance<AVAudioSessionPortDescription>()
                ?.firstOrNull { it.portType == AVAudioSessionPortBluetoothHFP }
        if (headset == null) {
            disable()
            return false
        }
        session.setPreferredInput(headset, null)
        return true
    }

    override fun disable() {
        headsetMicEnabled = false
        val session = AVAudioSession.sharedInstance()
        session.setPreferredInput(null, null)
        session.setCategory(AVAudioSessionCategoryPlayAndRecord, sessionOptions(), null)
    }
}
