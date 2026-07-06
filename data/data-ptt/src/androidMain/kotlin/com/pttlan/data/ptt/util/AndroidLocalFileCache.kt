package com.pttlan.data.ptt.util

import android.content.Context

class AndroidLocalFileCache(
    private val context: Context,
) : LocalFileCache {
    override fun getCacheDir(): String = context.cacheDir.absolutePath
}
