package com.pttlan.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.core.navigation.RootComponent
import com.pttlan.core.navigation.RootScreen
import com.pttlan.core.designsystem.theme.PttTheme
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val lifecycle = LifecycleRegistry()
    val rootComponent = RootComponent(
        componentContext = DefaultComponentContext(lifecycle = lifecycle)
    )

    return ComposeUIViewController {
        PttTheme {
            RootScreen(component = rootComponent)
        }
    }
}
