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

/** The watch's screens over the phone's navigation; each screen arrives with 25.3. */
@Composable
fun WearRoot(root: RootComponent) {
    val stack by root.childStack.subscribeAsState()
    MaterialTheme {
        AppScaffold {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stack.active.instance::class.simpleName.orEmpty())
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
