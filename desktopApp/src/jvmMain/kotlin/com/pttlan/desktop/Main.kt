package com.pttlan.desktop
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.material.Text

import org.koin.core.context.startKoin
import com.pttlan.core.di.appModules

fun main() {
    startKoin {
        modules(appModules())
    }
    application {
        Window(onCloseRequest = ::exitApplication, title = "PTT-LAN") {
            Text("Hello PTT-LAN Desktop")
        }
    }
}
