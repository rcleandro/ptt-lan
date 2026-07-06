package com.pttlan.core.datastore

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual class SettingsFactory {
    actual fun createSettings(): Settings {
        val delegate = Preferences.userRoot().node("ptt_lan_settings")
        return PreferencesSettings(delegate)
    }
}
