package com.pttlan.core.common.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs

class AndroidStorageInfoProvider(
    private val context: Context,
) : StorageInfoProvider {
    override fun getAvailableStorageOptions(): List<StorageOption> {
        val options = mutableListOf<StorageOption>()

        // Internal Storage
        val internalDir = context.filesDir
        val internalStat = StatFs(internalDir.path)
        val internalAvailableBytes = internalStat.availableBlocksLong * internalStat.blockSizeLong
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
                val externalStat = StatFs(sdCardDir.path)
                val externalAvailableBytes = externalStat.availableBlocksLong * externalStat.blockSizeLong
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
}
