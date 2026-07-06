package com.pttlan.core.datastore

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual class SettingsFactory(private val context: Context) {
    actual fun createSettings(): Settings {
        val delegate = context.getSharedPreferences("ptt_lan_settings", Context.MODE_PRIVATE)
        return SharedPreferencesSettings(delegate)
    }
}
