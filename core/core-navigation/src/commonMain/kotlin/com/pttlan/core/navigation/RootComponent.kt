package com.pttlan.core.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.Lifecycle
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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

    @OptIn(ExperimentalUuidApi::class)
    private val userId = Uuid.random().toString()

    private val connectionRepository: ConnectionRepository = get()
    private val settings: Settings = get()

    @OptIn(ExperimentalSettingsApi::class)
    val isCacheEnabled: StateFlow<Boolean> =
        (settings as? ObservableSettings)
            ?.getBooleanFlow("allow_cache", false)
            ?.stateIn(
                scope = lifecycle.coroutineScope(),
                started = SharingStarted.WhileSubscribed(),
                initialValue = settings.getBoolean("allow_cache", false),
            ) ?: MutableStateFlow(settings.getBoolean("allow_cache", false))

    val childStack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Connection,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    init {
        lifecycle.coroutineScope().launch {
            var wasConnected = false
            connectionRepository.connectionStatus.collect { status ->
                if (status == ConnectionStatus.Connected) {
                    wasConnected = true
                } else if (status == ConnectionStatus.Reconnecting && wasConnected) {
                    wasConnected = false
                    connectionRepository.disconnect()
                    navigation.navigate { listOf(Config.Connection) }

                    val activeChild = childStack.value.active.instance
                    if (activeChild is Child.ConnectionChild) {
                        activeChild.component.showError("Servidor desconectado")
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
                            parametersOf(context, config.channelId, userId)
                        },
                    )
                context.lifecycle.coroutineScope().launch {
                    component.effects.collect { effect ->
                        if (effect is PttEffect.NavigateBack) {
                            navigation.pop()
                        } else if (effect is PttEffect.NavigateToHistory) {
                            val nextConfig = Config.HistoryScreen(config.channelId)
                            navigation.navigate { stack ->
                                if (stack.lastOrNull() == nextConfig) stack else stack + nextConfig
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
                            parametersOf(context, config.channelId, { navigation.pop() })
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
        data class HistoryScreen(
            val channelId: String,
        ) : Config

        @Serializable
        data object Settings : Config
    }
}
