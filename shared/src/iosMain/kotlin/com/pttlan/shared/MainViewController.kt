package com.pttlan.shared

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.core.navigation.RootComponent
import com.pttlan.core.navigation.RootScreen
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.getIntFlow
import org.koin.core.context.GlobalContext
import platform.UIKit.UIViewController

@Suppress("FunctionName")
fun MainViewController(): UIViewController {
    val lifecycle = LifecycleRegistry()
    val rootComponent =
        RootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
        )

    return ComposeUIViewController(
        configure = {
            enforceStrictPlistSanityCheck = false
        },
    ) {
        val settings = GlobalContext.get().get<Settings>()
        val appThemeInt =
            if (settings is ObservableSettings) {
                settings.getIntFlow("app_theme", 0).collectAsState(initial = settings.getInt("app_theme", 0)).value
            } else {
                settings.getInt("app_theme", 0)
            }
        val appTheme = AppTheme.entries.getOrElse(appThemeInt) { AppTheme.SYSTEM }

        PttTheme(appTheme = appTheme) {
            RootScreen(component = rootComponent)
        }
    }
}
