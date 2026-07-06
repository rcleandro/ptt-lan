package com.pttlan.core.datastore

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual class SettingsFactory {
    actual fun createSettings(): Settings {
        val delegate = NSUserDefaults.standardUserDefaults
        return NSUserDefaultsSettings(delegate)
    }
}
