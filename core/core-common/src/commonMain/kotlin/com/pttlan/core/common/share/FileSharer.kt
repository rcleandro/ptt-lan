package com.pttlan.core.common.share

/** Hands a file to the system's share sheet, to send a recorded message to a chat, a mail or a drive. */
interface FileSharer {
    /** Where to write a file about to be shared, somewhere the platform can hand it out from. */
    val shareDirectory: String

    fun share(
        path: String,
        mimeType: String,
    )
}
