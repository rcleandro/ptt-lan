package com.pttlan.core.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.pttlan.core.datastore.SettingsDefaults
import com.pttlan.core.datastore.SettingsKeys
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.LocalServerHost
import com.pttlan.feature.channellist.ChannelListComponent
import com.pttlan.feature.channellist.ChannelListEffect
import com.pttlan.feature.connection.ConnectionComponent
import com.pttlan.feature.connection.ConnectionIntent
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

/**
 * Opens [channel] from the channel list. On a wide screen the list stays beside the open channel (27.2), so picking
 * another one replaces it instead of stacking a second channel on top; leaving it still goes back to the list.
 */
internal fun List<RootComponent.Config>.openingChannel(channel: RootComponent.Config.PttScreen): List<RootComponent.Config> =
    if (lastOrNull() == channel) this else takeWhile { it !is RootComponent.Config.PttScreen } + channel

class RootComponent(
    componentContext: ComponentContext,
) : ComponentContext by componentContext,
    KoinComponent {
    private val navigation = StackNavigation<Config>()

    private val connectionRepository: ConnectionRepository = get()
    private val localServerHost: LocalServerHost? = getKoin().getOrNull()
    private val settings: Settings = get()

    val isCacheEnabled: StateFlow<Boolean> = observeBoolean(SettingsKeys.ALLOW_CACHE, SettingsDefaults.ALLOW_CACHE)

    val reduceTransparency: StateFlow<Boolean> = observeBoolean(SettingsKeys.REDUCE_TRANSPARENCY, SettingsDefaults.REDUCE_TRANSPARENCY)

    @OptIn(ExperimentalSettingsApi::class)
    val appTheme: StateFlow<AppTheme> =
        (settings as? ObservableSettings)
            ?.getIntFlow(SettingsKeys.APP_THEME, SettingsDefaults.APP_THEME)
            ?.map(::toAppTheme)
            ?.stateIn(
                scope = lifecycle.coroutineScope(),
                started = SharingStarted.WhileSubscribed(),
                initialValue = toAppTheme(settings.getInt(SettingsKeys.APP_THEME, SettingsDefaults.APP_THEME)),
            ) ?: MutableStateFlow(toAppTheme(settings.getInt(SettingsKeys.APP_THEME, SettingsDefaults.APP_THEME)))

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

    /** Set once connected; a `Disconnected` after it is a drop, reported to the user. Cleared on a voluntary leave. */
    private var wasConnected = false

    init {
        lifecycle.coroutineScope().launch {
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
                        activeChild.component.showDisconnected(reason)
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
                    component.navigateToChannelList.collect {
                        navigation.navigate { stack ->
                            if (stack.contains(Config.ChannelList)) stack else stack + Config.ChannelList
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
                            navigation.navigate { stack -> stack.openingChannel(Config.PttScreen(effect.channelId)) }
                        } else if (effect is ChannelListEffect.Leave) {
                            leaveServer(endRoom = effect.endRoom)
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

    /** Leaving on purpose: no "Servidor desconectado" message, unlike a drop. */
    private fun leaveServer(endRoom: Boolean) {
        wasConnected = false
        connectionRepository.disconnect()
        // After the disconnect: this device is in its own room too, and would get its own "O host encerrou a sala".
        if (endRoom) localServerHost?.stop()
        navigation.navigate { listOf(Config.Connection) }

        // The connection screen was kept under the stack with the list it had, which still shows the room just
        // left (and, after ending a hosted room, a room that no longer exists).
        val activeChild = childStack.value.active.instance
        if (activeChild is Child.ConnectionChild) {
            activeChild.component.onIntent(ConnectionIntent.RefreshServers)
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
