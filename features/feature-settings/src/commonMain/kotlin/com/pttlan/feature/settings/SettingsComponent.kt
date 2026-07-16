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
        }
    }
}
