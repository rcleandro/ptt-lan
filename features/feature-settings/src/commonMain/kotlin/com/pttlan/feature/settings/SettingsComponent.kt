package com.pttlan.feature.settings

import com.arkivanov.decompose.ComponentContext
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsState(
    val nickname: String = ""
)

sealed interface SettingsIntent {
    data class UpdateNickname(val nickname: String) : SettingsIntent
}

class SettingsComponent(
    componentContext: ComponentContext,
    private val settings: Settings
) : ComponentContext by componentContext {

    private val _state = MutableStateFlow(SettingsState(nickname = settings.getString("nickname", "")))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.UpdateNickname -> {
                settings.putString("nickname", intent.nickname)
                _state.update { it.copy(nickname = intent.nickname) }
            }
        }
    }
}
