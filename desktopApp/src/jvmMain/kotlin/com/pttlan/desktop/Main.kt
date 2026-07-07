package com.pttlan.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.core.di.appModules
import com.pttlan.core.navigation.RootComponent
import com.pttlan.core.navigation.RootScreen
import org.koin.core.context.startKoin
import javax.swing.SwingUtilities

fun main() {
    startKoin {
        modules(appModules())
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
        Window(onCloseRequest = ::exitApplication, title = "PTT-LAN") {
            PttTheme {
                RootScreen(component = rootComponent!!)
            }
        }
    }
}
