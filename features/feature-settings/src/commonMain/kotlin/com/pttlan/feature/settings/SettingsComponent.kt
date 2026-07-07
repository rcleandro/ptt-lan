package com.pttlan.feature.settings

import com.arkivanov.decompose.ComponentContext
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsState(
    val nickname: String = "",
    val useOpus: Boolean = false,
    val useDarkTheme: Boolean = true,
)

sealed interface SettingsIntent {
    data class UpdateNickname(
        val nickname: String,
    ) : SettingsIntent

    data class ToggleOpus(
        val enabled: Boolean,
    ) : SettingsIntent

    data class ToggleTheme(
        val useDark: Boolean,
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
                useDarkTheme = settings.getBoolean("use_dark_theme", true),
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
            is SettingsIntent.ToggleTheme -> {
                settings.putBoolean("use_dark_theme", intent.useDark)
                _state.update { it.copy(useDarkTheme = intent.useDark) }
            }
        }
    }
}
