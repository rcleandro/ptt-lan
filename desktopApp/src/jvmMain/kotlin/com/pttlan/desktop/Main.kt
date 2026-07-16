package com.pttlan.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.core.di.appModules
import com.pttlan.core.navigation.RootComponent
import com.pttlan.core.navigation.RootScreen
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.getIntFlow
import org.koin.core.context.GlobalContext
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
        val settings = GlobalContext.get().get<Settings>()
        val appThemeInt =
            if (settings is ObservableSettings) {
                settings.getIntFlow("app_theme", 0).collectAsState(initial = settings.getInt("app_theme", 0)).value
            } else {
                settings.getInt("app_theme", 0)
            }
        val appTheme = AppTheme.entries.getOrElse(appThemeInt) { AppTheme.SYSTEM }

        Window(onCloseRequest = ::exitApplication, title = "PTT-LAN") {
            PttTheme(appTheme = appTheme) {
                RootScreen(component = rootComponent!!)
            }
        }
    }
}
