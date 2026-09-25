package com.pttlan.core.common.share

import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

/** The share sheet (`UIActivityViewController`) over the app's current screen. */
class IosFileSharer : FileSharer {
    override val shareDirectory: String = NSTemporaryDirectory()

    override fun share(
        path: String,
        mimeType: String,
    ) {
        @Suppress("DEPRECATION") // keyWindow: the app has a single window
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        val sheet = UIActivityViewController(listOf(NSURL.fileURLWithPath(path)), null)
        // On the iPad the sheet is a popover and needs something to point at
        sheet.popoverPresentationController?.sourceView = root.view
        root.presentViewController(sheet, animated = true, completion = null)
    }
}
