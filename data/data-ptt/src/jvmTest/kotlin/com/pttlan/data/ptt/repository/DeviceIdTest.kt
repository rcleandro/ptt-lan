package com.pttlan.data.ptt.repository

import com.pttlan.core.datastore.SettingsKeys
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DeviceIdTest {
    @Test
    fun keepsTheSameIdAcrossCalls() {
        val settings = MapSettings()

        val first = deviceId(settings)

        assertTrue(first.isNotBlank())
        assertEquals(first, deviceId(settings))
        assertEquals(first, settings.getStringOrNull(SettingsKeys.DEVICE_ID))
    }

    @Test
    fun differentInstallsGetDifferentIds() {
        assertNotEquals(deviceId(MapSettings()), deviceId(MapSettings()))
    }
}
