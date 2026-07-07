package com.pttlan.core.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.pttlan.domain.ptt.repository.ChannelRepository
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.VoiceRepository
import com.pttlan.feature.channellist.ChannelListComponent
import com.pttlan.feature.channellist.ChannelListEffect
import com.pttlan.feature.connection.ConnectionComponent
import com.pttlan.feature.connection.ConnectionEffect
import com.pttlan.feature.ptt.PttComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

import com.pttlan.core.network.PttWebSocketClient

class RootComponent(
    componentContext: ComponentContext,
    private val connectionRepository: ConnectionRepository,
    private val channelRepository: ChannelRepository,
    private val voiceRepository: VoiceRepository,
    private val webSocketClient: PttWebSocketClient,
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    private val userId = UUID.randomUUID().toString()
    private val scope = CoroutineScope(Dispatchers.Main)

    val childStack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Connection,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(config: Config, context: ComponentContext): Child {
        return when (config) {
            is Config.Connection -> {
                val component = ConnectionComponent(context, connectionRepository)
                scope.launch {
                    component.effects.collect { effect ->
                        if (effect is ConnectionEffect.NavigateToChannelList) {
                            navigation.push(Config.ChannelList)
                        }
                    }
                }
                Child.ConnectionChild(component)
            }
            is Config.ChannelList -> {
                val component = ChannelListComponent(context, channelRepository)
                scope.launch {
                    component.effects.collect { effect ->
                        if (effect is ChannelListEffect.NavigateToChannel) {
                            navigation.push(Config.PttScreen(effect.channelId))
                        }
                    }
                }
                Child.ChannelListChild(component)
            }
            is Config.PttScreen -> {
                Child.PttChild(
                    PttComponent(
                        componentContext = context,
                        channelId = config.channelId,
                        userId = userId,
                        voiceRepository = voiceRepository,
                        webSocketClient = webSocketClient,
                    )
                )
            }
        }
    }

    fun goBack() {
        navigation.pop()
    }

    sealed interface Child {
        class ConnectionChild(val component: ConnectionComponent) : Child
        class ChannelListChild(val component: ChannelListComponent) : Child
        class PttChild(val component: PttComponent) : Child
    }

    @Serializable
    sealed interface Config {
        @Serializable
        data object Connection : Config

        @Serializable
        data object ChannelList : Config

        @Serializable
        data class PttScreen(val channelId: String) : Config
    }
}
