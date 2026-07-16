@file:OptIn(ExperimentalForeignApi::class)

package com.pttlan.core.common.storage

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class IosStorageInfoProvider : StorageInfoProvider {
    override val isExternalStorageSupported: Boolean = false

    override fun getAvailableStorageOptions(): List<StorageOption> {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentDirectory = paths.firstOrNull() as? String ?: return emptyList()

        var availableSpace: Long = 0
        try {
            val attributes = NSFileManager.defaultManager.attributesOfFileSystemForPath(documentDirectory, null)
            val freeSize = attributes?.get(NSFileSystemFreeSize) as? NSNumber
            availableSpace = freeSize?.longLongValue ?: 0L
        } catch (_: Exception) {
            // Ignore
        }

        return listOf(
            StorageOption(
                id = "Interno",
                title = "Armazenamento local",
                availableSpaceBytes = availableSpace,
            ),
        )
    }

    private fun getCacheDir(): String? {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        return paths.firstOrNull() as? String
    }

    override fun getCacheUsageBytes(cacheLocationId: String): Long {
        val path = getCacheDir() ?: return 0L
        val fileManager = NSFileManager.defaultManager
        var totalSize = 0L
        val enumerator = fileManager.enumeratorAtPath(path)
        var file = enumerator?.nextObject() as? String
        while (file != null) {
            val fileAttributes = fileManager.attributesOfItemAtPath("$path/$file", null)
            val fileSize = fileAttributes?.get(NSFileSize) as? Long ?: 0L
            totalSize += fileSize
            file = enumerator?.nextObject() as? String
        }
        return totalSize
    }

    override fun clearCache(cacheLocationId: String) {
        val path = getCacheDir() ?: return
        val fileManager = NSFileManager.defaultManager
        val contents = fileManager.contentsOfDirectoryAtPath(path, null) as? List<String>
        contents?.forEach { file ->
            fileManager.removeItemAtPath("$path/$file", null)
        }
    }

    override fun getCacheDirPath(cacheLocationId: String): String? = getCacheDir()
}
