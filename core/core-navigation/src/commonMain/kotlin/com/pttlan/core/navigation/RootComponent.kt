package com.pttlan.core.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.pttlan.domain.ptt.repository.ChannelRepository
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.VoiceRepository
import com.pttlan.feature.channellist.ChannelListComponent
import com.pttlan.feature.channellist.ChannelListEffect
import com.pttlan.feature.connection.ConnectionComponent
import com.pttlan.feature.connection.ConnectionEffect
import com.pttlan.feature.ptt.PttComponent
import com.pttlan.feature.ptt.PttEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
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
    private val connectionRepository: ConnectionRepository,
    private val channelRepository: ChannelRepository,
    private val voiceRepository: VoiceRepository,
) : ComponentContext by componentContext,
    org.koin.core.component.KoinComponent {
    private val navigation = StackNavigation<Config>()

    @OptIn(ExperimentalUuidApi::class)
    private val userId = Uuid.random().toString()

    val childStack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Connection,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(
        config: Config,
        context: ComponentContext,
    ): Child =
        when (config) {
            is Config.Connection -> {
                val component = ConnectionComponent(context, connectionRepository)
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
                val component = ChannelListComponent(context, channelRepository)
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
                        }
                    }
                }
                Child.PttChild(component)
            }
        }

    fun goBack() {
        navigation.pop()
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
    }
}
