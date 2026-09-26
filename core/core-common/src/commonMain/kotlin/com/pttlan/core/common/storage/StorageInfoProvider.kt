package com.pttlan.core.common.storage

/** Id of the app's private storage. Saved in the settings, so it never changes. */
const val STORAGE_INTERNAL = "Interno"

/** Id of the external storage (an SD card, Android only). Saved in the settings, so it never changes. */
const val STORAGE_EXTERNAL = "Externo"

/** A place the history can be kept. The screen names it by [id], from its own texts. */
data class StorageOption(
    val id: String,
    val availableSpaceBytes: Long,
)

interface StorageInfoProvider {
    val isExternalStorageSupported: Boolean

    fun getAvailableStorageOptions(): List<StorageOption>

    fun getCacheUsageBytes(cacheLocationId: String): Long

    fun clearCache(cacheLocationId: String)

    fun getCacheDirPath(cacheLocationId: String): String?
}
