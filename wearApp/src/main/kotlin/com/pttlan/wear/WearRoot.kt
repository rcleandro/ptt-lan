package com.pttlan.wear

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.pttlan.core.navigation.RootComponent

/** The watch's screens over the phone's navigation (ADR 0011). */
@Composable
fun WearRoot(root: RootComponent) {
    val stack by root.childStack.subscribeAsState()
    MaterialTheme {
        AppScaffold {
            when (val child = stack.active.instance) {
                is RootComponent.Child.ConnectionChild -> WearConnectionScreen(child.component)
                is RootComponent.Child.ChannelListChild -> WearChannelListScreen(child.component)
                is RootComponent.Child.PttChild -> WearPttScreen(child.component)
                // History and settings are phone-only (ADR 0011); nothing on the watch navigates there
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Indisponível no relógio") }
            }
        }
    }
}

@Composable
fun WearPermissionWait() {
    MaterialTheme {
        AppScaffold {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Permita o microfone e a rede local para usar o PTT-LAN")
            }
        }
    }
}
