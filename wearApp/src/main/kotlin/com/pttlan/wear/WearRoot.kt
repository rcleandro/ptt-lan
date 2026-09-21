package com.pttlan.wear

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwipeToDismissBox
import androidx.wear.compose.material3.Text
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.pttlan.core.navigation.RootComponent
import com.pttlan.feature.channellist.ChannelListIntent
import com.pttlan.feature.ptt.PttIntent

/** The watch's screens over the phone's navigation (ADR 0011). */
@Composable
fun WearRoot(root: RootComponent) {
    val stack by root.childStack.subscribeAsState()
    WearPttTheme {
        AppScaffold {
            // Swiping right goes back one screen, the Wear OS way. On the first screen the system's own swipe
            // closes the app, which ends the session (MainActivity). The background stays empty: drawing the
            // previous screen would make it collect its component's effects a second time.
            SwipeToDismissBox(
                onDismissed = { goBack(root, stack.active.instance) },
                contentKey = stack.active.configuration,
                userSwipeEnabled = stack.backStack.isNotEmpty(),
            ) { isBackground ->
                if (!isBackground) WearScreen(stack.active.instance)
            }
        }
    }
}

@Composable
private fun WearScreen(child: RootComponent.Child) {
    when (child) {
        is RootComponent.Child.ConnectionChild -> WearConnectionScreen(child.component)
        is RootComponent.Child.ChannelListChild -> WearChannelListScreen(child.component)
        is RootComponent.Child.PttChild -> WearPttScreen(child.component)
        // History and settings are phone-only (ADR 0011); nothing on the watch navigates there
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Indisponível no relógio") }
    }
}

/** Back means leaving, as the screens' own buttons do: the channel from PTT, the server from the channel list. */
private fun goBack(
    root: RootComponent,
    child: RootComponent.Child,
) {
    when (child) {
        is RootComponent.Child.PttChild -> child.component.onIntent(PttIntent.LeaveChannel)
        is RootComponent.Child.ChannelListChild -> child.component.onIntent(ChannelListIntent.Leave)
        else -> root.goBack()
    }
}

@Composable
fun WearPermissionWait() {
    WearPttTheme {
        AppScaffold {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Permita o microfone e a rede local para usar o PTT-LAN")
            }
        }
    }
}
