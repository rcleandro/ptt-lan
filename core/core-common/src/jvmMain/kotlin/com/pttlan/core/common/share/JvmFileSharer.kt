package com.pttlan.core.common.share

import java.awt.Desktop
import java.io.File

/** The desktop has no share sheet: the file goes to Downloads and is shown there, in Finder or Explorer. */
class JvmFileSharer : FileSharer {
    override val shareDirectory: String
        get() = File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }.path

    override fun share(
        path: String,
        mimeType: String,
    ) {
        if (!Desktop.isDesktopSupported()) return
        val desktop = Desktop.getDesktop()
        val file = File(path)
        if (desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) desktop.browseFileDirectory(file) else desktop.open(file.parentFile)
    }
}
