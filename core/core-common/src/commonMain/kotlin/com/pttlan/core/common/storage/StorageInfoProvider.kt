package com.pttlan.core.common.storage

data class StorageOption(
    val id: String,
    val title: String,
    val availableSpaceBytes: Long,
)

interface StorageInfoProvider {
    val isExternalStorageSupported: Boolean

    fun getAvailableStorageOptions(): List<StorageOption>

    fun getCacheUsageBytes(cacheLocationId: String): Long

    fun clearCache(cacheLocationId: String)
}
