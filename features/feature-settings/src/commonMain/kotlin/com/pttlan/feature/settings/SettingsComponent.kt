package com.pttlan.feature.settings

import com.arkivanov.decompose.ComponentContext
import com.pttlan.core.designsystem.theme.AppTheme
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsState(
    val nickname: String = "",
    val useOpus: Boolean = false,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val alwaysListening: Boolean = true,
    val allowCache: Boolean = false,
    val cacheLocation: String = "Interno",
    val maxCacheSizeMb: Int = 500,
    val currentCacheUsageMb: Int = 125, // TODO: Obter uso real do diretório de cache
)

sealed interface SettingsIntent {
    data class UpdateNickname(
        val nickname: String,
    ) : SettingsIntent

    data class ToggleOpus(
        val enabled: Boolean,
    ) : SettingsIntent

    data class ChangeTheme(
        val theme: AppTheme,
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
) : ComponentContext by componentContext {
    private val _state =
        MutableStateFlow(
            SettingsState(
                nickname = settings.getString("nickname", ""),
                useOpus = settings.getBoolean("use_opus", false),
                appTheme = AppTheme.entries.getOrElse(settings.getInt("app_theme", 0)) { AppTheme.SYSTEM },
                alwaysListening = settings.getBoolean("always_listening", true),
                allowCache = settings.getBoolean("allow_cache", false),
                cacheLocation = settings.getString("cache_location", "Interno"),
                maxCacheSizeMb = settings.getInt("max_cache_size_mb", 500),
                currentCacheUsageMb = 125, // TODO: Obter uso real do diretório de cache
            ),
        )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.UpdateNickname -> {
                settings.putString("nickname", intent.nickname)
                _state.update { it.copy(nickname = intent.nickname) }
            }
            is SettingsIntent.ToggleOpus -> {
                settings.putBoolean("use_opus", intent.enabled)
                _state.update { it.copy(useOpus = intent.enabled) }
            }
            is SettingsIntent.ChangeTheme -> {
                settings.putInt("app_theme", intent.theme.ordinal)
                _state.update { it.copy(appTheme = intent.theme) }
            }
            is SettingsIntent.ToggleAlwaysListening -> {
                settings.putBoolean("always_listening", intent.enabled)
                _state.update { it.copy(alwaysListening = intent.enabled) }
            }
            is SettingsIntent.ToggleAllowCache -> {
                settings.putBoolean("allow_cache", intent.enabled)
                _state.update { it.copy(allowCache = intent.enabled) }
            }
            is SettingsIntent.ChangeCacheLocation -> {
                settings.putString("cache_location", intent.location)
                _state.update { it.copy(cacheLocation = intent.location) }
            }
            is SettingsIntent.ChangeMaxCacheSize -> {
                settings.putInt("max_cache_size_mb", intent.sizeMb)
                _state.update { it.copy(maxCacheSizeMb = intent.sizeMb) }
            }
            is SettingsIntent.ClearCache -> {
                // TODO: Limpar os arquivos de cache reais no disco
                _state.update { it.copy(currentCacheUsageMb = 0) }
            }
        }
    }
}
