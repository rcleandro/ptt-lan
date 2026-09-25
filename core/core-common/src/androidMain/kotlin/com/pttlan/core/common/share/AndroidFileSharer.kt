package com.pttlan.core.common.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** The system share sheet, reading the file through the app's `FileProvider` (`<package>.files`, cache `shared/`). */
class AndroidFileSharer(
    private val context: Context,
) : FileSharer {
    override val shareDirectory: String
        get() = File(context.cacheDir, "shared").apply { mkdirs() }.path

    override fun share(
        path: String,
        mimeType: String,
    ) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", File(path))
        val send =
            Intent(Intent.ACTION_SEND)
                .setType(mimeType)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // The chooser only passes the read grant on to the app picked when the uri is also in the clip data
        send.clipData = ClipData.newRawUri(null, uri)
        context.startActivity(
            Intent
                .createChooser(send, null)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }
}
