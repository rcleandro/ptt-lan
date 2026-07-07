package com.pttlan.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.core.di.appModules
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.feature.connection.ConnectionComponent
import com.pttlan.feature.connection.ConnectionScreen
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.pttlan.domain.ptt.repository.ChannelRepository
import com.pttlan.feature.channellist.ChannelListComponent
import com.pttlan.feature.channellist.ChannelListScreen
import com.pttlan.feature.channellist.ChannelListEffect
import com.pttlan.feature.connection.ConnectionEffect
import com.pttlan.feature.connection.ConnectionIntent

import com.pttlan.domain.ptt.repository.VoiceRepository
import com.pttlan.feature.ptt.PttComponent
import com.pttlan.feature.ptt.PttScreen
import java.util.UUID

import com.pttlan.core.navigation.RootComponent
import com.pttlan.core.navigation.RootScreen

import com.pttlan.core.network.PttWebSocketClient

class AppDeps : KoinComponent {
    val connectionRepository: ConnectionRepository by inject()
    val channelRepository: ChannelRepository by inject()
    val voiceRepository: VoiceRepository by inject()
    val webSocketClient: PttWebSocketClient by inject()
}

fun main() {
    startKoin {
        modules(appModules())
    }

    val lifecycle = LifecycleRegistry()
    val deps = AppDeps()
    val componentContext = DefaultComponentContext(lifecycle)

    val rootComponent = RootComponent(
        componentContext = componentContext,
        connectionRepository = deps.connectionRepository,
        channelRepository = deps.channelRepository,
        voiceRepository = deps.voiceRepository,
        webSocketClient = deps.webSocketClient
    )

    application {
        Window(onCloseRequest = ::exitApplication, title = "PTT-LAN") {
            PttTheme {
                RootScreen(component = rootComponent)
            }
        }
    }
}
