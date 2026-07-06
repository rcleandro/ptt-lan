package com.pttlan.desktop
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.material.Text

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "PTT-LAN") {
        Text("Hello PTT-LAN")
    }
}
