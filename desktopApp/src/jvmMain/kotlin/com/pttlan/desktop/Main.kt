package com.pttlan.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.pttlan.core.di.appModules
import org.koin.core.context.startKoin
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.feature.connection.ConnectionComponent
import com.pttlan.feature.connection.ConnectionScreen
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppDeps : KoinComponent {
    val connectionRepository: ConnectionRepository by inject()
}

fun main() {
    startKoin {
        modules(appModules())
    }

    val lifecycle = LifecycleRegistry()
    val deps = AppDeps()
    val componentContext = DefaultComponentContext(lifecycle)
    
    val connectionComponent = ConnectionComponent(
        componentContext = componentContext,
        connectionRepository = deps.connectionRepository
    )

    application {
        Window(onCloseRequest = ::exitApplication, title = "PTT-LAN") {
            PttTheme {
                ConnectionScreen(component = connectionComponent)
            }
        }
    }
}
