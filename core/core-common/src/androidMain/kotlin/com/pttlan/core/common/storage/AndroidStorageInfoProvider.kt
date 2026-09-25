package com.pttlan.core.common.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import java.io.File

class AndroidStorageInfoProvider(
    private val context: Context,
) : StorageInfoProvider {
    /**
     * Before Android 10 any app with the storage permission reads the external cache, and the history keeps
     * recorded voice there (30.6). From 10 on it is private to the app.
     */
    override val isExternalStorageSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun getAvailableStorageOptions(): List<StorageOption> {
        val options = mutableListOf<StorageOption>()

        // Internal Storage
        val internalDir = context.filesDir
        val internalAvailableBytes = getAvailableBytes(internalDir)
        options.add(
            StorageOption(
                id = "Interno",
                title = "Armazenamento interno",
                availableSpaceBytes = internalAvailableBytes,
            ),
        )

        // External Storage
        val externalDirs = context.getExternalFilesDirs(null)
        if (isExternalStorageSupported && externalDirs != null && externalDirs.size > 1) {
            val sdCardDir = externalDirs[1]
            if (sdCardDir != null && Environment.getExternalStorageState(sdCardDir) == Environment.MEDIA_MOUNTED) {
                val externalAvailableBytes = getAvailableBytes(sdCardDir)
                options.add(
                    StorageOption(
                        id = "Externo",
                        title = "Armazenamento externo",
                        availableSpaceBytes = externalAvailableBytes,
                    ),
                )
            }
        }

        return options
    }

    private fun getAvailableBytes(file: File): Long {
        try {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val uuid = storageManager.getUuidForPath(file)
            return storageManager.getAllocatableBytes(uuid)
        } catch (_: Exception) {
            // Fallback in case of any issues with UUID or StorageManager
        }
        val stat = StatFs(file.path)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    private fun getCacheDirForLocation(cacheLocationId: String): File? =
        // A setting saved as "Externo" before an update keeps to the private cache where external is not safe
        if (cacheLocationId == "Externo" && isExternalStorageSupported) {
            context.externalCacheDir
        } else {
            context.cacheDir
        }

    private fun getDirectorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0
        var size: Long = 0
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirectorySize(file) else file.length()
        }
        return size
    }

    override fun getCacheUsageBytes(cacheLocationId: String): Long {
        val dir = getCacheDirForLocation(cacheLocationId)
        return getDirectorySize(dir)
    }

    override fun clearCache(cacheLocationId: String) {
        val dir = getCacheDirForLocation(cacheLocationId)
        dir?.listFiles()?.forEach { it.deleteRecursively() }
    }

    override fun getCacheDirPath(cacheLocationId: String): String? = getCacheDirForLocation(cacheLocationId)?.absolutePath
}
