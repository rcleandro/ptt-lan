package com.pttlan.core.datastore

import com.russhwolf.settings.Settings

expect class SettingsFactory {
    fun createSettings(): Settings
}
