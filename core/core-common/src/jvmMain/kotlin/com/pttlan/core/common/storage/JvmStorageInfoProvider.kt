package com.pttlan.core.common.storage

import java.io.File

class JvmStorageInfoProvider : StorageInfoProvider {
    override val isExternalStorageSupported: Boolean = false

    @Suppress("UsableSpace")
    override fun getAvailableStorageOptions(): List<StorageOption> {
        val userHome = System.getProperty("user.home")
        val homeDir = File(userHome)

        return listOf(
            StorageOption(
                id = "Interno",
                title = "Armazenamento local",
                availableSpaceBytes = homeDir.usableSpace,
            ),
        )
    }

    private fun getCacheDir(): File {
        val userHome = System.getProperty("user.home")
        val cacheDir = File(userHome, ".ptt/cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        return cacheDir
    }

    private fun getDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0
        var size: Long = 0
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirectorySize(file) else file.length()
        }
        return size
    }

    override fun getCacheUsageBytes(cacheLocationId: String): Long = getDirectorySize(getCacheDir())

    override fun clearCache(cacheLocationId: String) {
        getCacheDir().listFiles()?.forEach { it.deleteRecursively() }
    }
}
