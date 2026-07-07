package com.pttlan.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.repository.ConnectionRepository
import org.koin.android.ext.android.inject
import com.pttlan.domain.ptt.repository.ChannelRepository
import com.pttlan.domain.ptt.repository.VoiceRepository
import com.pttlan.core.navigation.RootComponent
import com.pttlan.core.navigation.RootScreen

import com.pttlan.core.network.PttWebSocketClient

class MainActivity : ComponentActivity() {
    private val connectionRepository: ConnectionRepository by inject()
    private val channelRepository: ChannelRepository by inject()
    private val voiceRepository: VoiceRepository by inject()
    private val webSocketClient: PttWebSocketClient by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val componentContext = defaultComponentContext()
        val rootComponent = RootComponent(
            componentContext = componentContext,
            connectionRepository = connectionRepository,
            channelRepository = channelRepository,
            voiceRepository = voiceRepository,
            webSocketClient = webSocketClient
        )

        setContent {
            PttTheme {
                RootScreen(component = rootComponent)
            }
        }
    }
}
