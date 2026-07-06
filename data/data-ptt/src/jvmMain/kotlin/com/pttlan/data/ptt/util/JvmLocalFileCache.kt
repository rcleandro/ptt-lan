package com.pttlan.data.ptt.util

import java.io.File

class JvmLocalFileCache : LocalFileCache {
    override fun getCacheDir(): String {
        val os = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")
        val appName = "PTT-LAN"
        
        val dir = when {
            os.contains("win") -> File(System.getenv("AppData"), appName)
            os.contains("mac") -> File(userHome, "Library/Caches/$appName")
            else -> File(userHome, ".cache/$appName")
        }
        
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        return dir.absolutePath
    }
}
