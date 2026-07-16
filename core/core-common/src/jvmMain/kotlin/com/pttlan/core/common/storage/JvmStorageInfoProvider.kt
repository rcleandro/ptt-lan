package com.pttlan.core.common.storage

import java.io.File

class JvmStorageInfoProvider : StorageInfoProvider {
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
}
