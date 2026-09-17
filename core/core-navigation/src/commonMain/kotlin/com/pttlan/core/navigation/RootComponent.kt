package com.pttlan.core.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.feature.channellist.ChannelListComponent
import com.pttlan.feature.channellist.ChannelListEffect
import com.pttlan.feature.connection.ConnectionComponent
import com.pttlan.feature.connection.ConnectionEffect
import com.pttlan.feature.history.HistoryComponent
import com.pttlan.feature.ptt.PttComponent
import com.pttlan.feature.ptt.PttEffect
import com.pttlan.feature.ptt.PttIntent.PressPtt
import com.pttlan.feature.ptt.PttIntent.ReleasePtt
import com.pttlan.feature.settings.SettingsComponent
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getIntFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

private fun Lifecycle.coroutineScope(): CoroutineScope {
    val scope = CoroutineScope(Dispatchers.Main)
    subscribe(
        object : Lifecycle.Callbacks {
            override fun onDestroy() {
                scope.cancel()
            }
        },
    )
    return scope
}

class RootComponent(
    componentContext: ComponentContext,
) : ComponentContext by componentContext,
    KoinComponent {
    private val navigation = StackNavigation<Config>()

    private val connectionRepository: ConnectionRepository = get()
    private val settings: Settings = get()

    val isCacheEnabled: StateFlow<Boolean> = observeBoolean("allow_cache", false)

    val reduceTransparency: StateFlow<Boolean> = observeBoolean("reduce_transparency", false)

    @OptIn(ExperimentalSettingsApi::class)
    val appTheme: StateFlow<AppTheme> =
        (settings as? ObservableSettings)
            ?.getIntFlow("app_theme", 0)
            ?.map(::toAppTheme)
            ?.stateIn(
                scope = lifecycle.coroutineScope(),
                started = SharingStarted.WhileSubscribed(),
                initialValue = toAppTheme(settings.getInt("app_theme", 0)),
            ) ?: MutableStateFlow(toAppTheme(settings.getInt("app_theme", 0)))

    @OptIn(ExperimentalSettingsApi::class)
    private fun observeBoolean(
        key: String,
        default: Boolean,
    ): StateFlow<Boolean> =
        (settings as? ObservableSettings)
            ?.getBooleanFlow(key, default)
            ?.stateIn(
                scope = lifecycle.coroutineScope(),
                started = SharingStarted.WhileSubscribed(),
                initialValue = settings.getBoolean(key, default),
            ) ?: MutableStateFlow(settings.getBoolean(key, default))

    private fun toAppTheme(index: Int): AppTheme = AppTheme.entries.getOrElse(index) { AppTheme.SYSTEM }

    val childStack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Connection,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    /** Drives the "reconectando" badge; the screens keep showing while the client retries. */
    val connectionStatus: StateFlow<ConnectionStatus> = connectionRepository.connectionStatus

    init {
        lifecycle.coroutineScope().launch {
            var wasConnected = false
            connectionRepository.connectionStatus.collect { status ->
                // `Reconnecting` keeps the current screen: PttWebSocketClient is still retrying with backoff.
                // Only `Disconnected` (attempts exhausted or manual exit) sends the user back.
                if (status == ConnectionStatus.Connected) {
                    wasConnected = true
                } else if (status == ConnectionStatus.Disconnected && wasConnected) {
                    wasConnected = false
                    val reason = connectionRepository.lastDisconnectReason
                    connectionRepository.disconnect()
                    navigation.navigate { listOf(Config.Connection) }

                    val activeChild = childStack.value.active.instance
                    if (activeChild is Child.ConnectionChild) {
                        activeChild.component.showError(reason ?: "Servidor desconectado")
                    }
                }
            }
        }
    }

    private fun createChild(
        config: Config,
        context: ComponentContext,
    ): Child =
        when (config) {
            is Config.Connection -> {
                val component: ConnectionComponent = get(parameters = { parametersOf(context) })
                context.lifecycle.coroutineScope().launch {
                    component.effects.collect { effect ->
                        if (effect is ConnectionEffect.NavigateToChannelList) {
                            navigation.navigate { stack ->
                                if (stack.contains(Config.ChannelList)) stack else stack + Config.ChannelList
                            }
                        }
                    }
                }
                Child.ConnectionChild(component)
            }

            is Config.ChannelList -> {
                val component: ChannelListComponent = get(parameters = { parametersOf(context) })
                context.lifecycle.coroutineScope().launch {
                    component.effects.collect { effect ->
                        if (effect is ChannelListEffect.NavigateToChannel) {
                            val nextConfig = Config.PttScreen(effect.channelId)
                            navigation.navigate { stack ->
                                if (stack.lastOrNull() == nextConfig) stack else stack + nextConfig
                            }
                        }
                    }
                }
                Child.ChannelListChild(component)
            }

            is Config.PttScreen -> {
                val component: PttComponent =
                    get(
                        parameters = {
                            parametersOf(context, config.channelId, connectionRepository.sessionUserId.orEmpty())
                        },
                    )
                context.lifecycle.coroutineScope().launch {
                    component.effects.collect { effect ->
                        if (effect is PttEffect.NavigateBack) {
                            navigation.pop()
                        } else if (effect is PttEffect.NavigateToHistory) {
                            navigation.navigate { stack ->
                                if (stack.lastOrNull() == Config.HistoryScreen) stack else stack + Config.HistoryScreen
                            }
                        }
                    }
                }
                Child.PttChild(component)
            }

            is Config.HistoryScreen -> {
                val component: HistoryComponent =
                    get(
                        parameters = {
                            parametersOf(context, { navigation.pop() })
                        },
                    )
                Child.HistoryChild(component)
            }

            is Config.Settings -> {
                val component: SettingsComponent = get(parameters = { parametersOf(context) })
                Child.SettingsChild(component)
            }
        }

    fun goBack() {
        navigation.pop()
    }

    fun navigateToSettings() {
        navigation.navigate { stack ->
            if (stack.lastOrNull() is Config.Settings) stack else stack + Config.Settings
        }
    }

    fun navigateToHistory() {
        navigation.navigate { stack ->
            if (stack.lastOrNull() is Config.HistoryScreen) stack else stack + Config.HistoryScreen
        }
    }

    fun handlePttKey(isPressed: Boolean): Boolean {
        val activeChild = childStack.value.active.instance
        if (activeChild is Child.PttChild) {
            val intent = if (isPressed) PressPtt else ReleasePtt
            activeChild.component.onIntent(intent)
            return true
        }
        return false
    }

    sealed interface Child {
        class ConnectionChild(
            val component: ConnectionComponent,
        ) : Child

        class ChannelListChild(
            val component: ChannelListComponent,
        ) : Child

        class PttChild(
            val component: PttComponent,
        ) : Child

        class HistoryChild(
            val component: HistoryComponent,
        ) : Child

        class SettingsChild(
            val component: SettingsComponent,
        ) : Child
    }

    @Serializable
    sealed interface Config {
        @Serializable
        data object Connection : Config

        @Serializable
        data object ChannelList : Config

        @Serializable
        data class PttScreen(
            val channelId: String,
        ) : Config

        @Serializable
        data object HistoryScreen : Config

        @Serializable
        data object Settings : Config
    }
}
