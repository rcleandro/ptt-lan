package com.pttlan.feature.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.core.common.storage.StorageInfoProvider
import com.pttlan.core.common.storage.StorageOption
import com.pttlan.core.datastore.SettingsDefaults
import com.pttlan.core.datastore.SettingsKeys
import com.pttlan.core.designsystem.theme.AppTheme
import com.russhwolf.settings.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsComponentTest {
    private val lifecycle = LifecycleRegistry()

    private val settings: Settings = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { settings.getString(SettingsKeys.NICKNAME, "") } returns "TestUser"
        every { settings.getBoolean(SettingsKeys.USE_OPUS, SettingsDefaults.USE_OPUS) } returns false
        every { settings.getInt(SettingsKeys.APP_THEME, SettingsDefaults.APP_THEME) } returns 0
        every { settings.getBoolean(SettingsKeys.ALWAYS_LISTENING, SettingsDefaults.ALWAYS_LISTENING) } returns true
        every { settings.getBoolean(SettingsKeys.ALLOW_CACHE, SettingsDefaults.ALLOW_CACHE) } returns false
        every { settings.getString(SettingsKeys.CACHE_LOCATION, SettingsDefaults.CACHE_LOCATION) } returns "Interno"
        every { settings.getInt(SettingsKeys.MAX_CACHE_SIZE_MB, SettingsDefaults.MAX_CACHE_SIZE_MB) } returns 500
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class MockStorageInfoProvider : StorageInfoProvider {
        override val isExternalStorageSupported: Boolean = true

        override fun getAvailableStorageOptions(): List<StorageOption> =
            listOf(
                StorageOption("Interno", "Armazenamento interno", 10_000_000_000L),
                StorageOption("Externo", "Armazenamento externo", 50_000_000_000L),
            )

        override fun getCacheUsageBytes(cacheLocationId: String): Long = 1024 * 1024 * 125L // 125 MB

        override fun clearCache(cacheLocationId: String) {}

        override fun getCacheDirPath(cacheLocationId: String): String? = "/mock/cache"
    }

    private fun createComponent(): SettingsComponent {
        val componentContext = DefaultComponentContext(lifecycle)
        return SettingsComponent(componentContext, settings, MockStorageInfoProvider(), mockk(relaxed = true))
    }

    @Test
    fun `initialization loads settings`() =
        runTest(testDispatcher) {
            val component = createComponent()

            assertEquals("TestUser", component.state.value.nickname)
            assertEquals(false, component.state.value.useOpus)
            assertEquals(AppTheme.SYSTEM, component.state.value.appTheme)
            assertEquals(true, component.state.value.alwaysListening)
            assertEquals(false, component.state.value.allowCache)
            assertEquals("Interno", component.state.value.cacheLocation)
            assertEquals(500, component.state.value.maxCacheSizeMb)
        }

    @Test
    fun `UpdateNickname intent updates state and settings`() =
        runTest(testDispatcher) {
            val component = createComponent()

            component.onIntent(SettingsIntent.UpdateNickname("NewName"))

            assertEquals("NewName", component.state.value.nickname)
            verify(exactly = 1) { settings.putString(SettingsKeys.NICKNAME, "NewName") }
        }

    @Test
    fun `ToggleOpus intent updates state and settings`() =
        runTest(testDispatcher) {
            val component = createComponent()

            component.onIntent(SettingsIntent.ToggleOpus(true))

            assertEquals(true, component.state.value.useOpus)
            verify(exactly = 1) { settings.putBoolean(SettingsKeys.USE_OPUS, true) }
        }

    @Test
    fun `ChangeTheme intent updates state and settings`() =
        runTest(testDispatcher) {
            val component = createComponent()

            component.onIntent(SettingsIntent.ChangeTheme(AppTheme.DARK))

            assertEquals(AppTheme.DARK, component.state.value.appTheme)
            verify(exactly = 1) { settings.putInt(SettingsKeys.APP_THEME, AppTheme.DARK.ordinal) }
        }

    @Test
    fun `ToggleReduceTransparency intent updates state and settings`() =
        runTest(testDispatcher) {
            val component = createComponent()

            component.onIntent(SettingsIntent.ToggleReduceTransparency(true))

            assertEquals(true, component.state.value.reduceTransparency)
            verify(exactly = 1) { settings.putBoolean(SettingsKeys.REDUCE_TRANSPARENCY, true) }
        }

    @Test
    fun `ToggleAlwaysListening intent updates state and settings`() =
        runTest(testDispatcher) {
            val component = createComponent()

            component.onIntent(SettingsIntent.ToggleAlwaysListening(false))

            assertEquals(false, component.state.value.alwaysListening)
            verify(exactly = 1) { settings.putBoolean(SettingsKeys.ALWAYS_LISTENING, false) }
        }

    @Test
    fun `ToggleAllowCache intent updates state and settings`() =
        runTest(testDispatcher) {
            val component = createComponent()

            component.onIntent(SettingsIntent.ToggleAllowCache(true))

            assertEquals(true, component.state.value.allowCache)
            verify(exactly = 1) { settings.putBoolean(SettingsKeys.ALLOW_CACHE, true) }
        }

    @Test
    fun `ChangeCacheLocation intent updates state and settings`() =
        runTest(testDispatcher) {
            val component = createComponent()

            component.onIntent(SettingsIntent.ChangeCacheLocation("Externo"))

            assertEquals("Externo", component.state.value.cacheLocation)
            verify(exactly = 1) { settings.putString(SettingsKeys.CACHE_LOCATION, "Externo") }
        }

    @Test
    fun `ChangeMaxCacheSize intent updates state and settings`() =
        runTest(testDispatcher) {
            val component = createComponent()

            component.onIntent(SettingsIntent.ChangeMaxCacheSize(1024))

            assertEquals(1024, component.state.value.maxCacheSizeMb)
            verify(exactly = 1) { settings.putInt(SettingsKeys.MAX_CACHE_SIZE_MB, 1024) }
        }

    @Test
    fun `ClearCache intent updates state to reset usage`() =
        runTest(testDispatcher) {
            val component = createComponent()

            component.onIntent(SettingsIntent.ClearCache)

            assertEquals(0, component.state.value.currentCacheUsageMb)
        }
}
