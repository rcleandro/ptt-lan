package com.pttlan.core.common.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 30.6: before Android 10, any app with the storage permission reads the external cache, where the history
 * would keep recorded voice.
 */
@RunWith(AndroidJUnit4::class)
class AndroidStorageInfoProviderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    @Config(sdk = [28])
    fun `before android 10 the history never goes to external storage`() {
        val provider = AndroidStorageInfoProvider(context)

        assertFalse(provider.isExternalStorageSupported)
        assertTrue(provider.getAvailableStorageOptions().none { it.id == "Externo" })
        // A setting saved as "Externo" before the update falls back to the private cache
        assertEquals(context.cacheDir.absolutePath, provider.getCacheDirPath("Externo"))
    }

    @Test
    @Config(sdk = [29])
    fun `from android 10 on the external cache is private to the app and stays an option`() {
        val provider = AndroidStorageInfoProvider(context)

        assertTrue(provider.isExternalStorageSupported)
        assertEquals(context.externalCacheDir?.absolutePath, provider.getCacheDirPath("Externo"))
    }
}
