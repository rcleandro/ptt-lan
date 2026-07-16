package com.pttlan.core.common.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import java.io.File

class AndroidStorageInfoProvider(
    private val context: Context,
) : StorageInfoProvider {
    override val isExternalStorageSupported: Boolean = true

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
        if (externalDirs != null && externalDirs.size > 1) {
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
}
