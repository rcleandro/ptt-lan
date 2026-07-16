@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.pttlan.core.common.storage

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class IosStorageInfoProvider : StorageInfoProvider {
    override fun getAvailableStorageOptions(): List<StorageOption> {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentDirectory = paths.firstOrNull() as? String ?: return emptyList()

        var availableSpace: Long = 0
        try {
            val attributes = NSFileManager.defaultManager.attributesOfFileSystemForPath(documentDirectory, null)
            val freeSize = attributes?.get(NSFileSystemFreeSize) as? NSNumber
            availableSpace = freeSize?.longLongValue ?: 0L
        } catch (e: Exception) {
            // Ignore
        }

        return listOf(
            StorageOption(
                id = "Interno",
                title = "Armazenamento interno",
                availableSpaceBytes = availableSpace,
            ),
        )
    }
}
