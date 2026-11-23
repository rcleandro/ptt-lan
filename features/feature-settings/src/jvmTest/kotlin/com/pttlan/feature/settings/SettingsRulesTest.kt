package com.pttlan.feature.settings

import com.pttlan.core.common.storage.STORAGE_EXTERNAL
import com.pttlan.core.common.storage.STORAGE_INTERNAL
import com.pttlan.core.common.storage.StorageOption
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.settings_storage_external
import com.pttlan.core.designsystem.generated.resources.settings_storage_internal
import com.pttlan.core.designsystem.generated.resources.size_bytes
import com.pttlan.core.designsystem.generated.resources.size_gb
import com.pttlan.core.designsystem.generated.resources.size_kb
import com.pttlan.core.designsystem.generated.resources.size_mb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Rules the Compose and SwiftUI settings screens both draw (32.4). */
class SettingsRulesTest {
    @Test
    fun aSizeUsesTheLargestUnitThatKeepsItAtLeastOne() {
        assertEquals(SizeText(Res.string.size_bytes, 512), sizeText(512))
        assertEquals(SizeText(Res.string.size_kb, 2), sizeText(2_048))
        assertEquals(SizeText(Res.string.size_mb, 5), sizeText(5L * 1024 * 1024))
        assertEquals(SizeText(Res.string.size_gb, 2), sizeText(3L * 1024 * 1024 * 1024 / 2))
    }

    @Test
    fun theStoragePlaceIsNamedFromItsSavedId() {
        assertEquals(Res.string.settings_storage_external, storageName(STORAGE_EXTERNAL))
        assertEquals(Res.string.settings_storage_internal, storageName(STORAGE_INTERNAL))
    }

    @Test
    fun theUsageBarStaysBetweenEmptyAndFull() {
        assertEquals(0.25f, SettingsState(maxCacheSizeMb = 400, currentCacheUsageMb = 100).cacheUsage())
        assertEquals(1f, SettingsState(maxCacheSizeMb = 100, currentCacheUsageMb = 300).cacheUsage())
        assertEquals(0f, SettingsState(maxCacheSizeMb = 0, currentCacheUsageMb = 10).cacheUsage())
    }

    @Test
    fun aMissingSdCardIsReportedOnlyWhereTheDeviceTakesOne() {
        val internalOnly = listOf(StorageOption(STORAGE_INTERNAL, 0))
        assertTrue(SettingsState(isExternalStorageSupported = true, storageOptions = internalOnly).isSdCardMissing)
        assertFalse(SettingsState(isExternalStorageSupported = false, storageOptions = internalOnly).isSdCardMissing)
    }

    @Test
    fun theSliderStepsLandOnTheEnds() {
        assertEquals(0, (CACHE_SIZE_MAX_MB - CACHE_SIZE_MIN_MB) % CACHE_SIZE_STEP_MB)
    }
}
