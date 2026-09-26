package com.pttlan.feature.settings

import com.arkivanov.decompose.ComponentContext
import com.pttlan.core.common.storage.StorageInfoProvider
import com.pttlan.core.common.storage.StorageOption
import com.pttlan.core.datastore.SettingsDefaults
import com.pttlan.core.datastore.SettingsKeys
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.domain.ptt.repository.HistoryRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val BYTES_PER_MB = 1024L * 1024

data class SettingsState(
    val nickname: String = "",
    val useOpus: Boolean = SettingsDefaults.USE_OPUS,
    val useHeadsetMic: Boolean = SettingsDefaults.USE_HEADSET_MIC,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val reduceTransparency: Boolean = false,
    val alwaysListening: Boolean = true,
    val allowCache: Boolean = false,
    val cacheLocation: String = SettingsDefaults.CACHE_LOCATION,
    val maxCacheSizeMb: Int = 500,
    val currentCacheUsageMb: Int = 0,
    val storageOptions: List<StorageOption> = emptyList(),
    val isExternalStorageSupported: Boolean = false,
)

sealed interface SettingsIntent {
    data class UpdateNickname(
        val nickname: String,
    ) : SettingsIntent

    data class ToggleHeadsetMic(
        val enabled: Boolean,
    ) : SettingsIntent

    data class ToggleOpus(
        val enabled: Boolean,
    ) : SettingsIntent

    data class ChangeTheme(
        val theme: AppTheme,
    ) : SettingsIntent

    data class ToggleReduceTransparency(
        val enabled: Boolean,
    ) : SettingsIntent

    data class ToggleAlwaysListening(
        val enabled: Boolean,
    ) : SettingsIntent

    data class ToggleAllowCache(
        val enabled: Boolean,
    ) : SettingsIntent

    data class ChangeCacheLocation(
        val location: String,
    ) : SettingsIntent

    data class ChangeMaxCacheSize(
        val sizeMb: Int,
    ) : SettingsIntent

    data object ClearCache : SettingsIntent
}

class SettingsComponent(
    componentContext: ComponentContext,
    private val settings: Settings,
    private val storageInfoProvider: StorageInfoProvider,
    private val historyRepository: HistoryRepository,
) : ComponentContext by componentContext {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state =
        MutableStateFlow(
            SettingsState(
                nickname = settings.getString(SettingsKeys.NICKNAME, ""),
                useOpus = settings.getBoolean(SettingsKeys.USE_OPUS, SettingsDefaults.USE_OPUS),
                useHeadsetMic = settings.getBoolean(SettingsKeys.USE_HEADSET_MIC, SettingsDefaults.USE_HEADSET_MIC),
                appTheme =
                    AppTheme.entries.getOrElse(
                        settings.getInt(SettingsKeys.APP_THEME, SettingsDefaults.APP_THEME),
                    ) { AppTheme.SYSTEM },
                reduceTransparency = settings.getBoolean(SettingsKeys.REDUCE_TRANSPARENCY, SettingsDefaults.REDUCE_TRANSPARENCY),
                alwaysListening = settings.getBoolean(SettingsKeys.ALWAYS_LISTENING, SettingsDefaults.ALWAYS_LISTENING),
                allowCache = settings.getBoolean(SettingsKeys.ALLOW_CACHE, SettingsDefaults.ALLOW_CACHE),
                cacheLocation = settings.getString(SettingsKeys.CACHE_LOCATION, SettingsDefaults.CACHE_LOCATION),
                maxCacheSizeMb = settings.getInt(SettingsKeys.MAX_CACHE_SIZE_MB, SettingsDefaults.MAX_CACHE_SIZE_MB),
                currentCacheUsageMb =
                    (
                        storageInfoProvider.getCacheUsageBytes(
                            settings.getString(SettingsKeys.CACHE_LOCATION, SettingsDefaults.CACHE_LOCATION),
                        ) / BYTES_PER_MB
                    ).toInt(),
                storageOptions = storageInfoProvider.getAvailableStorageOptions(),
                isExternalStorageSupported = storageInfoProvider.isExternalStorageSupported,
            ),
        )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.UpdateNickname -> {
                settings.putString(SettingsKeys.NICKNAME, intent.nickname)
                _state.update { it.copy(nickname = intent.nickname) }
            }

            is SettingsIntent.ToggleHeadsetMic -> {
                settings.putBoolean(SettingsKeys.USE_HEADSET_MIC, intent.enabled)
                _state.update { it.copy(useHeadsetMic = intent.enabled) }
            }

            is SettingsIntent.ToggleOpus -> {
                settings.putBoolean(SettingsKeys.USE_OPUS, intent.enabled)
                _state.update { it.copy(useOpus = intent.enabled) }
            }

            is SettingsIntent.ChangeTheme -> {
                settings.putInt(SettingsKeys.APP_THEME, intent.theme.ordinal)
                _state.update { it.copy(appTheme = intent.theme) }
            }

            is SettingsIntent.ToggleReduceTransparency -> {
                settings.putBoolean(SettingsKeys.REDUCE_TRANSPARENCY, intent.enabled)
                _state.update { it.copy(reduceTransparency = intent.enabled) }
            }

            is SettingsIntent.ToggleAlwaysListening -> {
                settings.putBoolean(SettingsKeys.ALWAYS_LISTENING, intent.enabled)
                _state.update { it.copy(alwaysListening = intent.enabled) }
            }

            is SettingsIntent.ToggleAllowCache -> {
                settings.putBoolean(SettingsKeys.ALLOW_CACHE, intent.enabled)
                _state.update { it.copy(allowCache = intent.enabled) }
            }

            is SettingsIntent.ChangeCacheLocation -> {
                settings.putString(SettingsKeys.CACHE_LOCATION, intent.location)
                _state.update {
                    it.copy(
                        cacheLocation = intent.location,
                        currentCacheUsageMb = (storageInfoProvider.getCacheUsageBytes(intent.location) / BYTES_PER_MB).toInt(),
                    )
                }
            }

            is SettingsIntent.ChangeMaxCacheSize -> {
                settings.putInt(SettingsKeys.MAX_CACHE_SIZE_MB, intent.sizeMb)
                _state.update { it.copy(maxCacheSizeMb = intent.sizeMb) }
            }

            is SettingsIntent.ClearCache -> {
                scope.launch {
                    historyRepository.clearAllMessages()
                }
                storageInfoProvider.clearCache(_state.value.cacheLocation)
                _state.update { it.copy(currentCacheUsageMb = 0) }
            }
        }
    }
}
