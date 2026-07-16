package com.pttlan.core.common.storage

data class StorageOption(
    val id: String,
    val title: String,
    val availableSpaceBytes: Long,
)

interface StorageInfoProvider {
    fun getAvailableStorageOptions(): List<StorageOption>
}
