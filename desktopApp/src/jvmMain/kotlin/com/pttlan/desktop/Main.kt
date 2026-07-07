package com.pttlan.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.core.di.appModules
import com.pttlan.core.navigation.RootComponent
import com.pttlan.core.navigation.RootScreen
import com.pttlan.domain.ptt.repository.ChannelRepository
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.VoiceRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import javax.swing.SwingUtilities

class AppDeps : KoinComponent {
    val connectionRepository: ConnectionRepository by inject()
    val channelRepository: ChannelRepository by inject()
    val voiceRepository: VoiceRepository by inject()
}

fun main() {
    startKoin {
        modules(appModules())
    }

    val lifecycle = LifecycleRegistry()
    val deps = AppDeps()
    val componentContext = DefaultComponentContext(lifecycle)

    var rootComponent: RootComponent? = null
    SwingUtilities.invokeAndWait {
        rootComponent =
            RootComponent(
                componentContext = componentContext,
                connectionRepository = deps.connectionRepository,
                channelRepository = deps.channelRepository,
                voiceRepository = deps.voiceRepository,
            )
    }

    application {
        Window(onCloseRequest = ::exitApplication, title = "PTT-LAN") {
            PttTheme {
                RootScreen(component = rootComponent!!)
            }
        }
    }
}
