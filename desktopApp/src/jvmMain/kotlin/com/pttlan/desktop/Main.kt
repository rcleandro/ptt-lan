package com.pttlan.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.core.di.appModules
import com.pttlan.core.navigation.RootComponent
import com.pttlan.core.navigation.RootScreen
import com.pttlan.domain.ptt.repository.LocalServerHost
import org.koin.core.context.startKoin
import org.koin.dsl.module
import javax.swing.SwingUtilities

fun main() {
    val serverHost = DesktopServerHost()
    startKoin {
        modules(appModules() + module { single<LocalServerHost> { serverHost } })
    }

    val lifecycle = LifecycleRegistry()
    val componentContext = DefaultComponentContext(lifecycle)

    var rootComponent: RootComponent? = null
    SwingUtilities.invokeAndWait {
        rootComponent =
            RootComponent(
                componentContext = componentContext,
            )
    }

    application {
        // Netty threads are not daemons: without stopping the host, closing the window would leave the process alive
        Window(onCloseRequest = {
            serverHost.stop()
            exitApplication()
        }, title = "PTT-LAN") {
            RootScreen(component = rootComponent!!)
        }
    }
}
